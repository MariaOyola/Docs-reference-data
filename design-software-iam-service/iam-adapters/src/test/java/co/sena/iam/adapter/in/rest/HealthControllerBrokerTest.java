package co.sena.iam.adapter.in.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.iam.adapter.out.messaging.BrokerReadinessProbe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HU-IAM-011 E2: with the HU-IAM-007 relay's broker configured, {@code /ready} must also
 * reflect broker connectivity — a separate slice from {@link HealthControllerTest} because
 * the probe's presence in the context (not just its return value) drives the "si aplica"
 * behavior.
 */
@WebMvcTest(controllers = HealthController.class)
class HealthControllerBrokerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JdbcTemplate jdbc;

    @MockBean
    private BrokerReadinessProbe brokerReadinessProbe;

    @Test
    @DisplayName("GET /ready is 200 READY when both the DB and the configured broker respond")
    void readyWhenDbAndBrokerUp() throws Exception {
        given(jdbc.queryForObject("SELECT 1", Integer.class)).willReturn(1);
        given(brokerReadinessProbe.isUp()).willReturn(true);

        mvc.perform(get("/ready"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("READY"))
           .andExpect(jsonPath("$.db").value("ok"))
           .andExpect(jsonPath("$.broker").value("ok"));
    }

    @Test
    @DisplayName("GET /ready is 503 NOT_READY when the DB is up but the configured broker isn't")
    void notReadyWhenBrokerDown() throws Exception {
        given(jdbc.queryForObject("SELECT 1", Integer.class)).willReturn(1);
        given(brokerReadinessProbe.isUp()).willReturn(false);

        mvc.perform(get("/ready"))
           .andExpect(status().isServiceUnavailable())
           .andExpect(jsonPath("$.status").value("NOT_READY"))
           .andExpect(jsonPath("$.db").value("ok"))
           .andExpect(jsonPath("$.broker").value("down"));
    }
}
