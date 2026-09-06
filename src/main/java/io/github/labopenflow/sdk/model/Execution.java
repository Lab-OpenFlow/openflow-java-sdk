package io.github.labopenflow.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Execution(
    @JsonProperty("id") String id,
    @JsonProperty("workflow_id") String workflowId,
    @JsonProperty("status") String status,
    @JsonProperty("input") Map<String, Object> input,
    @JsonProperty("output") Map<String, Object> output,
    @JsonProperty("error") String error,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("steps") List<StepExecution> steps
) {}