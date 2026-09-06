package io.github.labopenflow.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.labopenflow.sdk.exception.OpenFlowApiException;
import io.github.labopenflow.sdk.exception.OpenFlowException;
import io.github.labopenflow.sdk.exception.OpenFlowTimeoutException;
import io.github.labopenflow.sdk.model.DLQMessage;
import io.github.labopenflow.sdk.model.DryRunResult;
import io.github.labopenflow.sdk.model.Execution;
import io.github.labopenflow.sdk.model.TaskItem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OpenFlowClientImpl implements OpenFlowClient {

    private final OpenFlowConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenFlowClientImpl(OpenFlowConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Constructor for testing with mock HttpClient
    public OpenFlowClientImpl(OpenFlowConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ================= Executions =================

    @Override
    public Execution execute(String workflowId, Map<String, Object> input) {
        return execute(workflowId, input, RequestOptions.empty());
    }

    @Override
    public Execution execute(String workflowId, Map<String, Object> input, RequestOptions options) {
        return executeAsync(workflowId, input, options).join();
    }

    @Override
    public CompletableFuture<Execution> executeAsync(String workflowId, Map<String, Object> input) {
        return executeAsync(workflowId, input, RequestOptions.empty());
    }

    @Override
    public CompletableFuture<Execution> executeAsync(String workflowId, Map<String, Object> input, RequestOptions options) {
        String path = "/api/v1/workflows/" + workflowId + "/execute";
        return sendRequestAsync("POST", path, input, options, Execution.class);
    }

    @Override
    public Execution getExecution(String executionId) {
        return getExecutionAsync(executionId).join();
    }

    @Override
    public CompletableFuture<Execution> getExecutionAsync(String executionId) {
        String path = "/api/v1/executions/" + executionId;
        return sendRequestAsync("GET", path, null, RequestOptions.empty(), Execution.class);
    }

    @Override
    public Execution waitForCompletion(String executionId, Duration timeout) {
        return waitForCompletionAsync(executionId, timeout).join();
    }

    @Override
    public CompletableFuture<Execution> waitForCompletionAsync(String executionId, Duration timeout) {
        CompletableFuture<Execution> future = new CompletableFuture<>();
        Instant deadline = Instant.now().plus(timeout);

        pollExecutionStatus(executionId, deadline, future);
        return future;
    }

    private void pollExecutionStatus(String executionId, Instant deadline, CompletableFuture<Execution> future) {
        if (Instant.now().isAfter(deadline)) {
            future.completeExceptionally(new OpenFlowTimeoutException("Timeout waiting for execution " + executionId + " to complete"));
            return;
        }

        getExecutionAsync(executionId).thenAccept(exec -> {
            String status = exec.status();
            if ("COMPLETED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                future.complete(exec);
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    future.completeExceptionally(e);
                    return;
                }
                pollExecutionStatus(executionId, deadline, future);
            }
        }).exceptionally(ex -> {
            future.completeExceptionally(ex);
            return null;
        });
    }

    // ================= Simulation & Dry-Run =================

    @Override
    public DryRunResult dryRun(String workflowId, Map<String, Object> input) {
        return dryRun(workflowId, input, Collections.emptyMap());
    }

    @Override
    public DryRunResult dryRun(String workflowId, Map<String, Object> input, Map<String, Object> mockOverrides) {
        return dryRunAsync(workflowId, input, mockOverrides).join();
    }

    @Override
    public CompletableFuture<DryRunResult> dryRunAsync(String workflowId, Map<String, Object> input, Map<String, Object> mockOverrides) {
        String path = "/api/v1/workflows/" + workflowId + "/dry-run";
        Map<String, Object> body = new HashMap<>();
        body.put("input", input);
        body.put("mock_overrides", mockOverrides);
        return sendRequestAsync("POST", path, body, RequestOptions.empty(), DryRunResult.class);
    }

    // ================= Worker Task Queues =================

    @Override
    public TaskItem pollTask(String queueName, String workerId, Duration timeout) {
        return pollTaskAsync(queueName, workerId, timeout).join();
    }

    @Override
    public CompletableFuture<TaskItem> pollTaskAsync(String queueName, String workerId, Duration timeout) {
        long timeoutSec = timeout.toSeconds() <= 0 ? 20 : timeout.toSeconds();
        String path = "/api/v1/task-queues/" + queueName + "/poll?worker_id=" + workerId + "&timeout=" + timeoutSec;
        return sendRequestAsync("POST", path, null, RequestOptions.empty(), TaskItem.class);
    }

    @Override
    public void heartbeatTask(String taskId, String workerId, String lockToken) {
        heartbeatTaskAsync(taskId, workerId, lockToken).join();
    }

    @Override
    public CompletableFuture<Void> heartbeatTaskAsync(String taskId, String workerId, String lockToken) {
        String path = "/api/v1/tasks/" + taskId + "/heartbeat";
        Map<String, String> body = Map.of("worker_id", workerId, "lock_token", lockToken);
        return sendRequestAsync("POST", path, body, RequestOptions.empty(), Void.class);
    }

    @Override
    public void completeTask(String taskId, String workerId, String lockToken, Map<String, Object> output) {
        completeTaskAsync(taskId, workerId, lockToken, output).join();
    }

    @Override
    public CompletableFuture<Void> completeTaskAsync(String taskId, String workerId, String lockToken, Map<String, Object> output) {
        String path = "/api/v1/tasks/" + taskId + "/complete";
        Map<String, Object> body = Map.of("worker_id", workerId, "lock_token", lockToken, "output", output);
        return sendRequestAsync("POST", path, body, RequestOptions.empty(), Void.class);
    }

    @Override
    public void failTask(String taskId, String workerId, String lockToken, String errorMessage) {
        failTaskAsync(taskId, workerId, lockToken, errorMessage).join();
    }

    @Override
    public CompletableFuture<Void> failTaskAsync(String taskId, String workerId, String lockToken, String errorMessage) {
        String path = "/api/v1/tasks/" + taskId + "/fail";
        Map<String, String> body = Map.of("worker_id", workerId, "lock_token", lockToken, "error_message", errorMessage);
        return sendRequestAsync("POST", path, body, RequestOptions.empty(), Void.class);
    }

    // ================= Dead Letter Queue (DLQ) =================

    @Override
    public List<DLQMessage> listDLQMessages(String status) {
        return listDLQMessagesAsync(status).join();
    }

    @Override
    public CompletableFuture<List<DLQMessage>> listDLQMessagesAsync(String status) {
        String path = "/api/v1/dlq" + (status != null && !status.isBlank() ? "?status=" + status : "");
        HttpRequest request = buildHttpRequest("GET", path, null, RequestOptions.empty());

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    checkError(response);
                    try {
                        Map<String, List<DLQMessage>> wrapper = objectMapper.readValue(
                                response.body(),
                                new TypeReference<Map<String, List<DLQMessage>>>() {}
                        );
                        return wrapper.getOrDefault("messages", Collections.emptyList());
                    } catch (IOException e) {
                        throw new OpenFlowException("Failed to parse DLQ response: " + e.getMessage(), e);
                    }
                });
    }

    @Override
    public String replayDLQMessage(String messageId) {
        return replayDLQMessageAsync(messageId).join();
    }

    @Override
    public CompletableFuture<String> replayDLQMessageAsync(String messageId) {
        String path = "/api/v1/dlq/" + messageId + "/replay";
        HttpRequest request = buildHttpRequest("POST", path, null, RequestOptions.empty());

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    checkError(response);
                    try {
                        Map<String, String> res = objectMapper.readValue(
                                response.body(),
                                new TypeReference<Map<String, String>>() {}
                        );
                        return res.getOrDefault("execution_id", "");
                    } catch (IOException e) {
                        throw new OpenFlowException("Failed to parse DLQ replay response: " + e.getMessage(), e);
                    }
                });
    }

    // ================= Webhooks & Approvals =================

    @Override
    public Map<String, Object> triggerWebhook(String path, Map<String, Object> payload, RequestOptions options) {
        return triggerWebhookAsync(path, payload, options).join();
    }

    @Override
    public CompletableFuture<Map<String, Object>> triggerWebhookAsync(String path, Map<String, Object> payload, RequestOptions options) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        if (cleanPath.startsWith("webhooks/")) {
            cleanPath = cleanPath.substring("webhooks/".length());
        }
        String endpoint = "/api/v1/webhooks/" + cleanPath;
        HttpRequest request = buildHttpRequest("POST", endpoint, payload, options);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    checkError(response);
                    try {
                        return objectMapper.readValue(
                                response.body(),
                                new TypeReference<Map<String, Object>>() {}
                        );
                    } catch (IOException e) {
                        throw new OpenFlowException("Failed to parse webhook response: " + e.getMessage(), e);
                    }
                });
    }

    @Override
    public void decideApproval(String requestId, String decision, String approver, String reason) {
        decideApprovalAsync(requestId, decision, approver, reason).join();
    }

    @Override
    public CompletableFuture<Void> decideApprovalAsync(String requestId, String decision, String approver, String reason) {
        String path = "/api/v1/approvals/" + requestId + "/decide";
        Map<String, String> body = Map.of(
                "decision", decision,
                "approver", approver,
                "reason", reason != null ? reason : ""
        );
        return sendRequestAsync("POST", path, body, RequestOptions.empty(), Void.class);
    }

    // ================= Internal HTTP Dispatcher =================

    private <T> CompletableFuture<T> sendRequestAsync(String method, String path, Object body, RequestOptions options, Class<T> responseType) {
        HttpRequest request = buildHttpRequest(method, path, body, options);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    checkError(response);
                    if (responseType == Void.class || response.statusCode() == 204) {
                        return null;
                    }
                    try {
                        return objectMapper.readValue(response.body(), responseType);
                    } catch (IOException e) {
                        throw new OpenFlowException("Failed to deserialize response to " + responseType.getSimpleName() + ": " + e.getMessage(), e);
                    }
                });
    }

    private HttpRequest buildHttpRequest(String method, String path, Object body, RequestOptions options) {
        URI uri = URI.create(config.getBaseUrl() + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(config.getTimeout());

        // Auth headers
        if (config.getBearerToken() != null && !config.getBearerToken().isBlank()) {
            builder.header("Authorization", "Bearer " + config.getBearerToken());
        } else if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header("X-API-Key", config.getApiKey());
        }

        // Custom RequestOptions headers
        if (options != null) {
            if (options.getIdempotencyKey() != null && !options.getIdempotencyKey().isBlank()) {
                builder.header("X-Idempotency-Key", options.getIdempotencyKey());
            }
            if (options.getCorrelationId() != null && !options.getCorrelationId().isBlank()) {
                builder.header("X-Correlation-ID", options.getCorrelationId());
            }
            for (Map.Entry<String, String> entry : options.getHeaders().entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        // Method & Body
        if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else if ("DELETE".equalsIgnoreCase(method)) {
            builder.DELETE();
        } else {
            byte[] bodyBytes = new byte[0];
            if (body != null) {
                try {
                    bodyBytes = objectMapper.writeValueAsBytes(body);
                } catch (IOException e) {
                    throw new OpenFlowException("Failed to serialize request body: " + e.getMessage(), e);
                }
                builder.header("Content-Type", "application/json");
            }
            builder.method(method.toUpperCase(), HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        }

        return builder.build();
    }

    private void checkError(HttpResponse<String> response) {
        int code = response.statusCode();
        if (code >= 400) {
            throw new OpenFlowApiException(code, response.body());
        }
    }
}