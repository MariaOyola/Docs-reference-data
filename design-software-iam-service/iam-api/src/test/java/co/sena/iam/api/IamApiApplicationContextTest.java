package co.sena.iam.api;

import co.sena.iam.application.port.out.AccessTokenIssuer;
import co.sena.iam.application.port.out.AccessTokenVerifier;
import co.sena.iam.application.port.out.LoginAuditRepository;
import co.sena.iam.application.port.out.OutboxPort;
import co.sena.iam.application.port.out.PasswordGenerator;
import co.sena.iam.application.port.out.PasswordHasher;
import co.sena.iam.application.port.out.PasswordResetRepository;
import co.sena.iam.application.port.out.RateLimiter;
import co.sena.iam.application.port.out.RbacCatalogRepository;
import co.sena.iam.application.port.out.RefreshTokenGenerator;
import co.sena.iam.application.port.out.RefreshTokenRepository;
import co.sena.iam.application.port.out.UserAccessRepository;
import co.sena.iam.application.port.out.UserQueryPort;
import co.sena.iam.application.port.out.UserRepository;
import co.sena.iam.application.port.out.UserRolePort;
import co.sena.iam.application.port.out.UserWritePort;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context smoke test.
 *
 * <p>Loads the complete Spring MVC handler mapping — the layer that catches
 * {@code AmbiguousServletMappingException}. Infrastructure ports are mocked so
 * no running database or broker is required.
 *
 * <p>Before this fix, context startup failed with:
 * <pre>
 *   Ambiguous mapping: cannot map 'sessionController' method
 *   GET /users/{id}/sessions to {... UserController ...}
 * </pre>
 * Root cause: {@code SessionController} (HU-002) and {@code UserController} (HU-005)
 * both implemented {@code UsersApi}, registering duplicate handlers for the two
 * session endpoints. Moving those endpoints to a dedicated {@code sessions} tag in
 * the OpenAPI contract generates a separate {@code SessionsApi} interface and
 * eliminates the conflict.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.MOCK,
        properties = {
            // "false" keeps @ConditionalOnProperty(spring.datasource.url) negative,
            // preventing PersistenceConfiguration (@EnableJpaRepositories) from activating.
            "spring.datasource.url=false",
            "spring.datasource.username=",
            "spring.datasource.password=",
            "iam.jwt.issuer=test-issuer",
            "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
        }
)
@AutoConfigureMockMvc
class IamApiApplicationContextTest {

    @DynamicPropertySource
    static void rsaKey(DynamicPropertyRegistry registry) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            String b64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
            registry.add("iam.jwt.private-key", () -> b64);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate RSA key for context test", e);
        }
    }

    // ---- Infrastructure ports (no DB/broker needed) -------------------------

    @MockBean UserRepository userRepository;
    @MockBean UserAccessRepository userAccessRepository;
    @MockBean LoginAuditRepository loginAuditRepository;
    @MockBean RefreshTokenRepository refreshTokenRepository;
    @MockBean PasswordHasher passwordHasher;
    @MockBean AccessTokenIssuer accessTokenIssuer;
    @MockBean RefreshTokenGenerator refreshTokenGenerator;
    @MockBean AccessTokenVerifier accessTokenVerifier;
    @MockBean PasswordResetRepository passwordResetRepository;
    @MockBean RateLimiter rateLimiter;
    @MockBean RbacCatalogRepository rbacCatalogRepository;
    @MockBean OutboxPort outboxPort;
    @MockBean PasswordGenerator passwordGenerator;
    @MockBean UserWritePort userWritePort;
    @MockBean UserRolePort userRolePort;
    @MockBean UserQueryPort userQueryPort;
    @MockBean JdbcTemplate jdbcTemplate;

    @Autowired MockMvc mvc;

    @Test
    void applicationContextLoads() {
        // Passes only when the full MVC handler mapping contains no ambiguous routes.
    }

    /**
     * Dispatches through the real {@code DispatcherServlet} against the full handler mapping,
     * for every {@code @RestController} tag ({@code auth}, {@code modules}, {@code reports}).
     * A 401 (missing token, caught by {@code GetCurrentUser}) proves the route resolved to its
     * controller; a 404 here means the mapping is missing or a class failed to register —
     * {@code applicationContextLoads()} alone would not catch that, since a controller simply
     * absent from the handler mapping does not fail context startup the way an ambiguous
     * mapping does.
     */
    @Test
    void everyTaggedEndpointRoutesToItsController() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/modules")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/reports/login-audit")).andExpect(status().isUnauthorized());
    }
}
