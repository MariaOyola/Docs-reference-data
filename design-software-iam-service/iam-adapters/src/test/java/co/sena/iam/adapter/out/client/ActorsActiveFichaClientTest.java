package co.sena.iam.adapter.out.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActorsActiveFichaClientTest {
    private static final UUID USER_ID = UUID.fromString("d1000000-0000-0000-0000-000000000001");
    private static final UUID FICHA_ID = UUID.fromString("d3000000-0000-0000-0000-000000000001");

    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void omitsFichaWhenActorsReturnsNoContent() throws Exception {
        assertStatusProducesEmpty(204);
    }

    @Test
    void omitsFichaWhenActorsReturnsNotFound() throws Exception {
        assertStatusProducesEmpty(404);
    }

    @Test
    void omitsFichaWhenActorsReturnsConflict() throws Exception {
        assertStatusProducesEmpty(409);
    }

    @Test
    void omitsFichaWhenActorsReturnsServerError() throws Exception {
        assertStatusProducesEmpty(500);
    }

    @Test
    void omitsFichaWhenActorsIsUnavailable() {
        ActorsActiveFichaClient client = new ActorsActiveFichaClient(
                "http://127.0.0.1:1/internal/learners/by-user/{user_id}/active-ficha",
                "", "", "", "");

        assertTrue(client.findActiveFicha(USER_ID).isEmpty());
    }

    @Test
    void returnsFichaForValidActorsResponse() throws Exception {
        server.createContext("/active-ficha", exchange -> {
            byte[] body = ("{\"learner_id\":\"" + USER_ID + "\",\"ficha_id\":\"" + FICHA_ID + "\"}")
                    .getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ActorsActiveFichaClient client = new ActorsActiveFichaClient(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/active-ficha?user={user_id}",
                "", "", "", "");

        assertEquals(Optional.of(FICHA_ID), client.findActiveFicha(USER_ID));
    }

    private void assertStatusProducesEmpty(int status) throws Exception {
        server.createContext("/active-ficha", exchange -> {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();

        ActorsActiveFichaClient client = new ActorsActiveFichaClient(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/active-ficha?user={user_id}",
                "", "", "", "");

        assertTrue(client.findActiveFicha(USER_ID).isEmpty());
    }
}
