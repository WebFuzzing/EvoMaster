package org.evomaster.arazzo.models.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.evomaster.arazzo.deserializer.FailureReusableDeserializer;
import org.evomaster.arazzo.deserializer.ParameterReusableDeserializer;
import org.evomaster.arazzo.deserializer.SuccessReusableDeserializer;
import org.evomaster.arazzo.models.domain.FailureReusable;
import org.evomaster.arazzo.models.domain.ParameterReusable;
import org.evomaster.arazzo.models.domain.SuccessReusable;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;

public class WorkflowDTO {
    private String workflowId;
    private String summary;
    private String description;
    private Schema<?> inputs;
    private List<String> dependsOn;
    private List<StepDTO> steps;
    private Map<String, String> outputs;

    @JsonDeserialize(contentUsing = SuccessReusableDeserializer.class)
    private List<SuccessReusable> successActions;

    @JsonDeserialize(contentUsing = FailureReusableDeserializer.class)
    private List<FailureReusable> failureActions;

    @JsonDeserialize(contentUsing = ParameterReusableDeserializer.class)
    private List<ParameterReusable> parameters;

    public WorkflowDTO() {
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Schema<?> getInputs() {
        return inputs;
    }

    public void setInputs(Schema<?> inputs) {
        this.inputs = inputs;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public List<StepDTO> getSteps() {
        return steps;
    }

    public void setSteps(List<StepDTO> steps) {
        this.steps = steps;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, String> outputs) {
        this.outputs = outputs;
    }

    public List<SuccessReusable> getSuccessActions() {
        return successActions;
    }

    public void setSuccessActions(List<SuccessReusable> successActions) {
        this.successActions = successActions;
    }

    public List<FailureReusable> getFailureActions() {
        return failureActions;
    }

    public void setFailureActions(List<FailureReusable> failureActions) {
        this.failureActions = failureActions;
    }

    public List<ParameterReusable> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterReusable> parameters) {
        this.parameters = parameters;
    }
}
