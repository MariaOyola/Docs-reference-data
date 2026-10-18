package co.sena.iam.adapter.in.rest;

import co.sena.iam.adapter.out.messaging.BrokerReadinessProbe;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;
    private final Optional<BrokerReadinessProbe> brokerReadinessProbe;

    HealthController(JdbcTemplate jdbc, Optional<BrokerReadinessProbe> brokerReadinessProbe) {
        this.jdbc = jdbc;
        this.brokerReadinessProbe = brokerReadinessProbe;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    // HU-IAM-011: readiness = DB always, broker only when the HU-IAM-007 relay is configured
    // (brokerReadinessProbe is empty otherwise — see BrokerHealthConfiguration).
    @GetMapping("/ready")
    ResponseEntity<Map<String, String>> ready() {
        Map<String, String> body = new LinkedHashMap<>();
        boolean ready = true;

        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            body.put("db", "ok");
        } catch (Exception e) {
            ready = false;
            body.put("db", "down");
        }

        if (brokerReadinessProbe.isPresent()) {
            boolean brokerUp = brokerReadinessProbe.get().isUp();
            body.put("broker", brokerUp ? "ok" : "down");
            ready = ready && brokerUp;
        }

        body.put("status", ready ? "READY" : "NOT_READY");
        return ready ? ResponseEntity.ok(body) : ResponseEntity.status(503).body(body);
    }
}
