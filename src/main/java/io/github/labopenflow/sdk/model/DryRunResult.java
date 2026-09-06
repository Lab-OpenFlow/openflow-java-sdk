package io.github.labopenflow.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DryRunResult(
    @JsonProperty("workflow_id") String workflowId,
    @JsonProperty("status") String status,
    @JsonProperty("steps") List<DryRunStep> steps,
    @JsonProperty("final_output") Map<String, Object> finalOutput
) {}