package co.sena.iam.adapter.in.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HU-IAM-011 E2: liveness always reports UP; readiness reflects the DB. No broker is
 * configured in this slice (no {@code spring.rabbitmq.host}), so the readiness probe stays
 * absent and {@code /ready} never mentions a "broker" component — see {@link
 * HealthControllerBrokerTest} for the broker-configured case.
 */
@WebMvcTest(controllers = HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("GET /health always reports UP, regardless of dependencies")
    void healthAlwaysUp() throws Exception {
        mvc.perform(get("/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /ready is 200 READY when the DB responds, no broker configured")
    void readyWhenDbUp() throws Exception {
        given(jdbc.queryForObject("SELECT 1", Integer.class)).willReturn(1);

        mvc.perform(get("/ready"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("READY"))
           .andExpect(jsonPath("$.db").value("ok"))
           .andExpect(jsonPath("$.broker").doesNotExist());
    }

    @Test
    @DisplayName("GET /ready is 503 NOT_READY when the DB doesn't respond")
    void notReadyWhenDbDown() throws Exception {
        willThrow(new DataAccessResourceFailureException("connection refused"))
                .given(jdbc).queryForObject("SELECT 1", Integer.class);

        mvc.perform(get("/ready"))
           .andExpect(status().isServiceUnavailable())
           .andExpect(jsonPath("$.status").value("NOT_READY"))
           .andExpect(jsonPath("$.db").value("down"));
    }
}
