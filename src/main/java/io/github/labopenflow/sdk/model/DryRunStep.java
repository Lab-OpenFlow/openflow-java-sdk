package io.github.labopenflow.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DryRunStep(
    @JsonProperty("step_id") String stepId,
    @JsonProperty("name") String name,
    @JsonProperty("type") String type,
    @JsonProperty("status") String status,
    @JsonProperty("output") Map<String, Object> output
) {}