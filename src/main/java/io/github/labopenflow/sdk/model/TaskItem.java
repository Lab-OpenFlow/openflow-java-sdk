package io.github.labopenflow.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskItem(
    @JsonProperty("id") String id,
    @JsonProperty("queue_name") String queueName,
    @JsonProperty("workflow_id") String workflowId,
    @JsonProperty("execution_id") String executionId,
    @JsonProperty("stage_id") String stageId,
    @JsonProperty("payload") Map<String, Object> payload,
    @JsonProperty("lock_token") String lockToken,
    @JsonProperty("lease_expires_at") Instant leaseExpiresAt
) {}