package io.github.labopenflow.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StepExecution(
    @JsonProperty("step_id") String stepId,
    @JsonProperty("name") String name,
    @JsonProperty("type") String type,
    @JsonProperty("status") String status,
    @JsonProperty("input") Map<String, Object> input,
    @JsonProperty("output") Map<String, Object> output,
    @JsonProperty("error") String error,
    @JsonProperty("attempt") int attempt,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("completed_at") Instant completedAt
) {}