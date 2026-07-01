package com.webfuzzing.arazzo.models.unresolved;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.webfuzzing.arazzo.deserializer.FailureReusableDeserializer;
import com.webfuzzing.arazzo.deserializer.ParameterReusableDeserializer;
import com.webfuzzing.arazzo.deserializer.SuccessReusableDeserializer;
import com.webfuzzing.arazzo.models.domain.FailureReusable;
import com.webfuzzing.arazzo.models.domain.ParameterReusable;
import com.webfuzzing.arazzo.models.domain.SuccessReusable;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Jackson-deserializable representation of a Workflow Object
 * with unresolved references. Mutable intermediate model used during parsing;
 * mapped to the immutable domain {@link com.webfuzzing.arazzo.models.domain.Workflow}
 * by {@link com.webfuzzing.arazzo.mapper.ArazzoMapper}.
 * Uses {@link SuccessReusable}, {@link FailureReusable} and {@link ParameterReusable}
 * for representing (Object | Reusable Object).
 */
public class UnresolvedWorkflow {
    private String workflowId;
    private String summary;
    private String description;
    private Schema<?> inputs;
    private List<String> dependsOn;
    private List<UnresolvedStep> steps;
    private Map<String, String> outputs;

    @JsonDeserialize(contentUsing = SuccessReusableDeserializer.class)
    private List<SuccessReusable> successActions;

    @JsonDeserialize(contentUsing = FailureReusableDeserializer.class)
    private List<FailureReusable> failureActions;

    @JsonDeserialize(contentUsing = ParameterReusableDeserializer.class)
    private List<ParameterReusable> parameters;

    public UnresolvedWorkflow() {
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

    public List<UnresolvedStep> getSteps() {
        return steps;
    }

    public void setSteps(List<UnresolvedStep> steps) {
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
