package co.sena.iam.api;

import co.sena.iam.adapter.in.rest.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class IamApiApplicationSmokeTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JdbcTemplate jdbc;

    @Test
    void healthEndpointReturns200() throws Exception {
        mvc.perform(get("/health"))
           .andExpect(status().isOk());
    }
}
