package io.github.labopenflow.sdk;

import com.sun.net.httpserver.HttpServer;
import io.github.labopenflow.sdk.exception.OpenFlowApiException;
import io.github.labopenflow.sdk.model.DLQMessage;
import io.github.labopenflow.sdk.model.DryRunResult;
import io.github.labopenflow.sdk.model.Execution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OpenFlowClientTest {

    private static HttpServer server;
    private static OpenFlowClient client;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        // Dry-run endpoint
        server.createContext("/api/v1/workflows/test-wf/dry-run", exchange -> {
            String json = """
                {
                    "workflow_id": "test-wf",
                    "status": "COMPLETED",
                    "steps": [
                        {
                            "step_id": "calc",
                            "name": "Calculate Total",
                            "type": "transform",
                            "status": "COMPLETED",
                            "output": {"total": 500}
                        }
                    ],
                    "final_output": {"total": 500}
                }
                """;
            sendResponse(exchange, 200, json);
        });

        // Execute endpoint
        server.createContext("/api/v1/workflows/test-wf/execute", exchange -> {
            assertEquals("Bearer secret-token", exchange.getRequestHeaders().getFirst("Authorization"));
            assertEquals("idemp-123", exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));

            String json = """
                {
                    "id": "exec-100",
                    "workflow_id": "test-wf",
                    "status": "PENDING"
                }
                """;
            sendResponse(exchange, 200, json);
        });

        // Execution status polling endpoint
        AtomicInteger pollCount = new AtomicInteger(0);
        server.createContext("/api/v1/executions/exec-100", exchange -> {
            int count = pollCount.incrementAndGet();
            String status = count > 1 ? "COMPLETED" : "RUNNING";
            String json = String.format("""
                {
                    "id": "exec-100",
                    "workflow_id": "test-wf",
                    "status": "%s",
                    "output": {"result": "success"}
                }
                """, status);
            sendResponse(exchange, 200, json);
        });

        // DLQ endpoints
        server.createContext("/api/v1/dlq", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/replay")) {
                sendResponse(exchange, 200, "{\"execution_id\":\"exec-replay-999\"}");
                return;
            }
            String json = """
                {
                    "messages": [
                        {
                            "id": "dlq-msg-1",
                            "workflow_id": "test-wf",
                            "status": "PENDING"
                        }
                    ]
                }
                """;
            sendResponse(exchange, 200, json);
        });

        // Error endpoint
        server.createContext("/api/v1/workflows/failing-wf/execute", exchange -> {
            sendResponse(exchange, 404, "{\"error\": \"workflow not found\"}");
        });

        server.start();

        int port = server.getAddress().getPort();
        OpenFlowConfig config = OpenFlowConfig.builder()
                .baseUrl("http://localhost:" + port)
                .bearerToken("secret-token")
                .timeout(Duration.ofSeconds(5))
                .build();

        client = OpenFlowClient.create(config);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testExecuteAndPollCompletion() {
        RequestOptions opts = RequestOptions.withIdempotencyKey("idemp-123");
        Execution exec = client.execute("test-wf", Map.of("amount", 250), opts);

        assertNotNull(exec);
        assertEquals("exec-100", exec.id());
        assertEquals("PENDING", exec.status());

        Execution completed = client.waitForCompletion(exec.id(), Duration.ofSeconds(3));
        assertNotNull(completed);
        assertEquals("COMPLETED", completed.status());
        assertEquals("success", completed.output().get("result"));
    }

    @Test
    void testDryRun() {
        DryRunResult result = client.dryRun("test-wf", Map.of("amount", 250));
        assertNotNull(result);
        assertEquals("COMPLETED", result.status());
        assertFalse(result.steps().isEmpty());
        assertEquals("calc", result.steps().get(0).stepId());
        assertEquals(500, ((Number) result.steps().get(0).output().get("total")).intValue());
    }

    @Test
    void testDLQListAndReplay() {
        List<DLQMessage> messages = client.listDLQMessages("PENDING");
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("dlq-msg-1", messages.get(0).id());

        String replayId = client.replayDLQMessage("dlq-msg-1");
        assertEquals("exec-replay-999", replayId);
    }

    @Test
    void testApiErrorHandling() {
        OpenFlowApiException ex = assertThrows(OpenFlowApiException.class, () -> {
            client.execute("failing-wf", Map.of());
        });

        assertEquals(404, ex.getStatusCode());
        assertTrue(ex.getResponseBody().contains("workflow not found"));
    }

    private static void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}