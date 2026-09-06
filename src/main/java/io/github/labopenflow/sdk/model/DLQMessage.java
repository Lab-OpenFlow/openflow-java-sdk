package io.github.labopenflow.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DLQMessage(
    @JsonProperty("id") String id,
    @JsonProperty("workflow_id") String workflowId,
    @JsonProperty("execution_id") String executionId,
    @JsonProperty("source") String source,
    @JsonProperty("topic_or_path") String topicOrPath,
    @JsonProperty("payload") Map<String, Object> payload,
    @JsonProperty("error_message") String errorMessage,
    @JsonProperty("retry_count") int retryCount,
    @JsonProperty("status") String status,
    @JsonProperty("created_at") Instant createdAt
) {}