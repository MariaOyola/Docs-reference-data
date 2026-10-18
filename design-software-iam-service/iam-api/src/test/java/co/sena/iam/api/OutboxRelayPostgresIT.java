package co.sena.iam.api;

import static org.assertj.core.api.Assertions.assertThat;

import co.sena.iam.adapter.in.rest.UserApplicationService;
import co.sena.iam.adapter.out.messaging.OutboxRelay;
import co.sena.iam.application.port.in.AssignRoleCommand;
import co.sena.iam.application.port.in.AuthenticateUserUseCase;
import co.sena.iam.application.port.in.CreateUserCommand;
import co.sena.iam.application.port.in.LoginCommand;
import co.sena.iam.application.port.in.UserCreateResult;
import co.sena.iam.domain.model.ActorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * HU-IAM-007 end to end against a real PostgreSQL (iam-db schemas) and a real RabbitMQ broker.
 *
 * <p>Proves the outbox path: a login stages {@code iam.session.started} and creating a user
 * stages {@code iam.user.created}; the relay then publishes both to the {@code iam-events}
 * topic exchange as shared-contracts envelopes and marks the rows {@code published_at}.
 *
 * <p>Drives the use cases directly ({@code webEnvironment = NONE}) rather than through the HTTP
 * layer, so it exercises the outbox path without depending on the web mapping.
 *
 * <pre>
 * IAM_DB_DSN=jdbc:postgresql://localhost:15432/design-software-develop \
 * IAM_DB_USERNAME=design_software_app IAM_DB_PASSWORD=... \
 * IAM_BROKER_HOST=localhost IAM_BROKER_USERNAME=app IAM_BROKER_PASSWORD=... ./mvnw verify
 * </pre>
 *
 * <p>Rows use the {@link #ADMIN_EMAIL}/{@link #TARGET_EMAIL} markers and are deleted by those
 * markers alone — no blanket truncation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "IAM_DB_DSN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "IAM_BROKER_HOST", matches = ".+")
class OutboxRelayPostgresIT {

    private static final String ADMIN_EMAIL = "hu-iam-007-admin@test.sena.edu.co";
    private static final String TARGET_EMAIL = "hu-iam-007-target@test.sena.edu.co";
    private static final String PASSWORD = "Integr4tion!Test";
    private static final long RECEIVE_TIMEOUT_MS = 5_000;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthenticateUserUseCase authenticateUser;

    @Autowired
    private UserApplicationService userApplicationService;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private TopicExchange iamEventsExchange;

    // The @RestController beans are still created in this non-web context; they take a
    // request-scoped HttpServletRequest that has no real binding here. A mock satisfies the
    // constructor — the controllers are never exercised (this IT drives the use cases directly).
    @MockBean
    private HttpServletRequest servletRequest;

    private final ObjectMapper json = new ObjectMapper();

    private UUID adminId;
    private String queueName;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("iam.jwt.private-key", OutboxRelayPostgresIT::ephemeralSigningKey);
        registry.add("iam.jwt.issuer", () -> envOr("IAM_JWT_ISSUER", "https://iam.it.sena.local"));
        registry.add("spring.rabbitmq.host", () -> System.getenv("IAM_BROKER_HOST"));
        registry.add("spring.rabbitmq.port", () -> envOr("IAM_BROKER_PORT", "5672"));
        registry.add("spring.rabbitmq.username", () -> envOr("IAM_BROKER_USERNAME", "app"));
        registry.add("spring.rabbitmq.password", () -> envOr("IAM_BROKER_PASSWORD", "app"));
        registry.add("spring.rabbitmq.virtual-host", () -> envOr("IAM_BROKER_VHOST", "/"));
        // Park the background scheduler far away so the test drives the relay explicitly.
        registry.add("iam.outbox.relay.poll-interval-ms", () -> "3600000");
        registry.add("iam.outbox.relay.initial-delay-ms", () -> "3600000");
    }

    @BeforeEach
    void setUp() {
        deleteTestData();

        // A dedicated queue bound to iam-events with "iam.#" catches every iam.* event published.
        // Non-exclusive/non-auto-delete so it survives broker connection churn between declare and
        // receive; deleted explicitly in tearDown.
        queueName = "hu-iam-007-it." + UUID.randomUUID();
        Queue queue = new Queue(queueName, false, false, false);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(iamEventsExchange).with("iam.#"));

        adminId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity."user"
                    (id, email, password_hash, first_name, last_name, actor_type, is_active)
                VALUES (?, ?, ?, 'It', 'Admin', 'USER', TRUE)
                """, adminId, ADMIN_EMAIL, new BCryptPasswordEncoder().encode(PASSWORD));
    }

    @AfterEach
    void tearDown() {
        if (queueName != null) {
            amqpAdmin.deleteQueue(queueName);
        }
        deleteTestData();
    }

    @Test
    @DisplayName("E1-E4: the relay publishes every staged iam.* event to iam-events as a conformant envelope and marks it published")
    void relayPublishesStagedEventsAndMarksThemPublished() throws Exception {
        // E4 — a successful login stages iam.session.started.
        authenticateUser.authenticate(LoginCommand.of(ADMIN_EMAIL, PASSWORD));
        // E1 — creating a user stages iam.user.created.
        UserCreateResult created = userApplicationService.createUser(new CreateUserCommand(
                TARGET_EMAIL, "Ana", "Ríos", ActorType.INSTRUCTOR, null, null, null));
        UUID createdUserId = created.id();
        // E3 — assigning a role stages iam.role.assigned.
        userApplicationService.assignRole(new AssignRoleCommand(createdUserId, adminId, "INSTRUCTOR", null, null));
        // E2 — deactivating stages iam.user.deactivated.
        userApplicationService.deactivateUser(createdUserId);

        String sessionEventId = eventIdOf("iam.session.started", adminId.toString());
        String userCreatedEventId = eventIdOf("iam.user.created", createdUserId.toString());
        String roleAssignedEventId = eventIdOf("iam.role.assigned", createdUserId.toString());
        String deactivatedEventId = eventIdOf("iam.user.deactivated", createdUserId.toString());
        assertThat(sessionEventId).as("E4 session.started staged").isNotNull();
        assertThat(userCreatedEventId).as("E1 user.created staged").isNotNull();
        assertThat(roleAssignedEventId).as("E3 role.assigned staged").isNotNull();
        assertThat(deactivatedEventId).as("E2 user.deactivated staged").isNotNull();

        int published = relay.pollAndPublish();
        assertThat(published).as("all four staged events (at least) are published").isGreaterThanOrEqualTo(4);

        Map<String, JsonNode> received = drainByEventId();
        assertThat(received).containsKeys(sessionEventId, userCreatedEventId, roleAssignedEventId, deactivatedEventId);

        // Every wire envelope conforms to shared-contracts: business data under `payload`, never `data`.
        for (String eventId : java.util.List.of(
                sessionEventId, userCreatedEventId, roleAssignedEventId, deactivatedEventId)) {
            JsonNode envelope = received.get(eventId);
            assertThat(envelope.has("data")).as("no legacy data key on the wire").isFalse();
            assertThat(envelope.has("payload")).as("payload present").isTrue();
            assertThat(envelope.get("event_id").asText()).isEqualTo(eventId);
            assertThat(envelope.get("source_service").asText()).isEqualTo("iam");
            assertThat(publishedAtIsSet(eventId)).as("published_at stamped for " + eventId).isTrue();
        }
        assertThat(received.get(sessionEventId).get("event_type").asText()).isEqualTo("iam.session.started");
        assertThat(received.get(sessionEventId).get("payload").get("user_id").asText()).isEqualTo(adminId.toString());
        assertThat(received.get(userCreatedEventId).get("payload").get("user_id").asText())
                .isEqualTo(createdUserId.toString());
        assertThat(received.get(roleAssignedEventId).get("payload").get("role_name").asText()).isEqualTo("INSTRUCTOR");

        // Idempotency: a second pass re-publishes none of our events.
        relay.pollAndPublish();
        Map<String, JsonNode> secondPass = drainByEventId();
        assertThat(secondPass).doesNotContainKeys(
                sessionEventId, userCreatedEventId, roleAssignedEventId, deactivatedEventId);
    }

    // ---------------------------------------------------------------- helpers

    /** Reads every message currently on the test queue, keyed by the AMQP message id (= event_id). */
    private Map<String, JsonNode> drainByEventId() throws Exception {
        Map<String, JsonNode> byId = new HashMap<>();
        Message message;
        boolean first = true;
        while ((message = rabbitTemplate.receive(queueName, first ? RECEIVE_TIMEOUT_MS : 500)) != null) {
            first = false;
            String messageId = message.getMessageProperties().getMessageId();
            byId.put(messageId, json.readTree(message.getBody()));
        }
        return byId;
    }

    private String eventIdOf(String eventType, String userId) {
        return jdbc.query("""
                SELECT event_id::text FROM identity.outbox
                WHERE event_type = ? AND payload->'data'->>'user_id' = ?
                ORDER BY created_at DESC LIMIT 1
                """, rs -> rs.next() ? rs.getString(1) : null, eventType, userId);
    }

    private boolean publishedAtIsSet(String eventId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM identity.outbox WHERE event_id = ?::uuid AND published_at IS NOT NULL",
                Long.class, eventId);
        return count != null && count == 1;
    }

    private void deleteTestData() {
        for (UUID id : jdbc.queryForList(
                "SELECT id FROM identity.\"user\" WHERE email IN (?, ?)", UUID.class, ADMIN_EMAIL, TARGET_EMAIL)) {
            jdbc.update("DELETE FROM identity.outbox WHERE payload->'data'->>'user_id' = ?", id.toString());
            jdbc.update("DELETE FROM session.refresh_token WHERE user_id = ?", id);
            jdbc.update("DELETE FROM rbac.user_role WHERE user_id = ?", id);
        }
        jdbc.update("DELETE FROM identity.outbox WHERE event_type = 'iam.user.created' AND payload->'data'->>'email' = ?", TARGET_EMAIL);
        jdbc.update("DELETE FROM identity_audit.audit_login WHERE email_attempted IN (?, ?)", ADMIN_EMAIL, TARGET_EMAIL);
        jdbc.update("DELETE FROM identity.\"user\" WHERE email IN (?, ?)", ADMIN_EMAIL, TARGET_EMAIL);
    }

    private static String ephemeralSigningKey() {
        String configured = System.getenv("IAM_JWT_PRIVATE_KEY");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        try {
            java.security.KeyPairGenerator gen = java.security.KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return java.util.Base64.getEncoder().encodeToString(gen.generateKeyPair().getPrivate().getEncoded());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA required by JRE spec", e);
        }
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
