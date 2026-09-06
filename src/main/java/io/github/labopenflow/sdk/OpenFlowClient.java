package io.github.labopenflow.sdk;

import io.github.labopenflow.sdk.model.DLQMessage;
import io.github.labopenflow.sdk.model.DryRunResult;
import io.github.labopenflow.sdk.model.Execution;
import io.github.labopenflow.sdk.model.TaskItem;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface OpenFlowClient {

    static OpenFlowClient create(String baseUrl) {
        return builder().baseUrl(baseUrl).build();
    }

    static OpenFlowClient create(OpenFlowConfig config) {
        return new OpenFlowClientImpl(config);
    }

    static OpenFlowConfig.Builder builder() {
        return OpenFlowConfig.builder();
    }

    // ================= Executions =================

    Execution execute(String workflowId, Map<String, Object> input);

    Execution execute(String workflowId, Map<String, Object> input, RequestOptions options);

    CompletableFuture<Execution> executeAsync(String workflowId, Map<String, Object> input);

    CompletableFuture<Execution> executeAsync(String workflowId, Map<String, Object> input, RequestOptions options);

    Execution getExecution(String executionId);

    CompletableFuture<Execution> getExecutionAsync(String executionId);

    Execution waitForCompletion(String executionId, Duration timeout);

    CompletableFuture<Execution> waitForCompletionAsync(String executionId, Duration timeout);

    // ================= Simulation & Dry-Run =================

    DryRunResult dryRun(String workflowId, Map<String, Object> input);

    DryRunResult dryRun(String workflowId, Map<String, Object> input, Map<String, Object> mockOverrides);

    CompletableFuture<DryRunResult> dryRunAsync(String workflowId, Map<String, Object> input, Map<String, Object> mockOverrides);

    // ================= Worker Task Queues =================

    TaskItem pollTask(String queueName, String workerId, Duration timeout);

    CompletableFuture<TaskItem> pollTaskAsync(String queueName, String workerId, Duration timeout);

    void heartbeatTask(String taskId, String workerId, String lockToken);

    CompletableFuture<Void> heartbeatTaskAsync(String taskId, String workerId, String lockToken);

    void completeTask(String taskId, String workerId, String lockToken, Map<String, Object> output);

    CompletableFuture<Void> completeTaskAsync(String taskId, String workerId, String lockToken, Map<String, Object> output);

    void failTask(String taskId, String workerId, String lockToken, String errorMessage);

    CompletableFuture<Void> failTaskAsync(String taskId, String workerId, String lockToken, String errorMessage);

    // ================= Dead Letter Queue (DLQ) =================

    List<DLQMessage> listDLQMessages(String status);

    CompletableFuture<List<DLQMessage>> listDLQMessagesAsync(String status);

    String replayDLQMessage(String messageId);

    CompletableFuture<String> replayDLQMessageAsync(String messageId);

    // ================= Webhooks & Approvals =================

    Map<String, Object> triggerWebhook(String path, Map<String, Object> payload, RequestOptions options);

    CompletableFuture<Map<String, Object>> triggerWebhookAsync(String path, Map<String, Object> payload, RequestOptions options);

    void decideApproval(String requestId, String decision, String approver, String reason);

    CompletableFuture<Void> decideApprovalAsync(String requestId, String decision, String approver, String reason);
}