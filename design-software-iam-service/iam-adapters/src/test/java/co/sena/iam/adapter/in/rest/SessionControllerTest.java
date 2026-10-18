package co.sena.iam.adapter.in.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.iam.application.port.in.ListUserSessionsUseCase;
import co.sena.iam.application.port.in.RevokeUserSessionUseCase;
import co.sena.iam.domain.exception.SessionNotFoundException;
import co.sena.iam.domain.model.RefreshTokenRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract for HU-IAM-002 E3: list and revoke sessions. */
@WebMvcTest(controllers = SessionController.class)
class SessionControllerTest {

    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final Instant CREATED = Instant.parse("2026-08-25T10:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-08-25T10:30:00Z");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ListUserSessionsUseCase listSessions;

    @MockBean
    private RevokeUserSessionUseCase revokeSession;

    private static RefreshTokenRecord activeSession() {
        return RefreshTokenRecord.newSession(
                SESSION_ID, USER_ID, "hash", "Chrome/Windows", "10.0.0.1", EXPIRES, CREATED);
    }

    // --- E3 list ---

    @Test
    @DisplayName("E3 GET /users/{id}/sessions returns 200 with all active sessions")
    void listSessionsReturns200WithSessions() throws Exception {
        given(listSessions.listSessions(USER_ID)).willReturn(List.of(activeSession()));

        mvc.perform(get("/api/v1/users/{id}/sessions", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$[0].device_hint").value("Chrome/Windows"))
                .andExpect(jsonPath("$[0].ip_address").value("10.0.0.1"))
                .andExpect(jsonPath("$[0].is_revoked").value(false));
    }

    @Test
    @DisplayName("E3 GET /users/{id}/sessions returns 200 with empty list when no active sessions")
    void listSessionsReturns200WhenEmpty() throws Exception {
        given(listSessions.listSessions(USER_ID)).willReturn(List.of());

        mvc.perform(get("/api/v1/users/{id}/sessions", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- E3 revoke ---

    @Test
    @DisplayName("E3 DELETE /users/{id}/sessions/{session_id} revokes the session and returns 204")
    void revokeSessionReturns204() throws Exception {
        mvc.perform(delete("/api/v1/users/{id}/sessions/{session_id}", USER_ID, SESSION_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("E3 DELETE /users/{id}/sessions/{session_id} for unknown session returns 404")
    void revokeUnknownSessionReturns404() throws Exception {
        UUID unknown = UUID.fromString("deadbeef-0000-0000-0000-000000000000");
        willThrow(new SessionNotFoundException(unknown))
                .given(revokeSession).revokeSession(USER_ID, unknown);

        mvc.perform(delete("/api/v1/users/{id}/sessions/{session_id}", USER_ID, unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("IAM_SESSION_NOT_FOUND"));
    }
}
