package co.sena.iam.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.iam.adapter.out.security.JwtProperties;
import co.sena.iam.adapter.out.security.M2mAccessTokenIssuer;
import co.sena.iam.adapter.out.security.M2mClientProperties;
import co.sena.iam.adapter.out.security.RsaSigningKeys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class M2mTokenControllerTest {

    private static final String CLIENT_ID = "academic-management-service";
    private static final String CLIENT_SECRET = "local-test-only-secret";
    private static final String ISSUER = "https://iam.test.sena.edu.co";
    private static final Instant TEST_NOW = Instant.now();
    private static RsaSigningKeys keys;

    @BeforeAll
    static void setUpKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        keys = new RsaSigningKeys(new JwtProperties(privateKey, ISSUER, "iam-test", null, null));
    }

    private MockMvc mvc(M2mClientProperties client) {
        var issuer = new M2mAccessTokenIssuer(keys, client,
                Clock.fixed(TEST_NOW, ZoneOffset.UTC));
        return MockMvcBuilders.standaloneSetup(new M2mTokenController(client, issuer)).build();
    }

    private static String basic(String clientId, String secret) {
        return "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + secret).getBytes());
    }

    @Test
    void issuesShortLivedTokenWithOnlyAcademicCapacityFeatures() throws Exception {
        var client = new M2mClientProperties(CLIENT_ID, CLIENT_SECRET, 300);

        var result = mvc(client).perform(post("/api/v1/auth/token")
                        .header("Authorization", basic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(300))
                .andExpect(jsonPath("$.scope").value(
                        "ACT_INTERNAL_FICHA_LEARNER_COUNT:GLOBAL ACADEMIC_FICHA_VIEW:GLOBAL"))
                .andReturn();

        String accessToken = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString()).get("access_token").asText();
        Claims claims = Jwts.parser().verifyWith(keys.publicKey())
                .clock(() -> Date.from(TEST_NOW)).build().parseSignedClaims(accessToken).getPayload();

        org.assertj.core.api.Assertions.assertThat(claims.get("features", List.class))
                .containsExactlyElementsOf(M2mAccessTokenIssuer.ACADEMIC_FEATURES);
        org.assertj.core.api.Assertions.assertThat(claims.get("token_use", String.class)).isEqualTo("service");
        org.assertj.core.api.Assertions.assertThat(claims.getExpiration().toInstant())
                .isEqualTo(TEST_NOW.truncatedTo(ChronoUnit.SECONDS).plusSeconds(300));
    }

    @Test
    void rejectsWrongClientSecret() throws Exception {
        mvc(new M2mClientProperties(CLIENT_ID, CLIENT_SECRET, 300))
                .perform(post("/api/v1/auth/token")
                        .header("Authorization", basic(CLIENT_ID, "wrong-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void rejectsOtherGrantTypes() throws Exception {
        mvc(new M2mClientProperties(CLIENT_ID, CLIENT_SECRET, 300))
                .perform(post("/api/v1/auth/token")
                        .header("Authorization", basic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    @Test
    void refusesToIssueTokenWhenClientIsNotConfigured() throws Exception {
        mvc(new M2mClientProperties("", "", 300))
                .perform(post("/api/v1/auth/token")
                        .header("Authorization", basic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }
}
