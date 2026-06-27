package com.webfuzzing.arazzo.models.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.webfuzzing.arazzo.deserializer.FailureReusableDeserializer;
import com.webfuzzing.arazzo.deserializer.ParameterReusableDeserializer;
import com.webfuzzing.arazzo.deserializer.SuccessReusableDeserializer;
import com.webfuzzing.arazzo.models.domain.*;

import java.util.List;
import java.util.Map;

/**
 * Representing the Step (DTO)
 * Used for direct document parsing
 * Use SuccessReusable, FailureReusable and ParameterReusable, for representing (Object | Reusable Object)
 */
public class StepDTO {
    private String description;
    private String stepId;
    private String operationId;
    private String operationPath;
    private String workflowId;
    private RequestBody requestBody;
    private List<Criterion> successCriteria;
    private Map<String, String> outputs;

    @JsonDeserialize(contentUsing = ParameterReusableDeserializer.class)
    private List<ParameterReusable> parameters;

    @JsonDeserialize(contentUsing = SuccessReusableDeserializer.class)
    private List<SuccessReusable> onSuccess;

    @JsonDeserialize(contentUsing = FailureReusableDeserializer.class)
    private List<FailureReusable> onFailure;

    public StepDTO() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getOperationPath() {
        return operationPath;
    }

    public void setOperationPath(String operationPath) {
        this.operationPath = operationPath;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    public List<Criterion> getSuccessCriteria() {
        return successCriteria;
    }

    public void setSuccessCriteria(List<Criterion> successCriteria) {
        this.successCriteria = successCriteria;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, String> outputs) {
        this.outputs = outputs;
    }

    public List<ParameterReusable> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterReusable> parameters) {
        this.parameters = parameters;
    }

    public List<SuccessReusable> getOnSuccess() {
        return onSuccess;
    }

    public void setOnSuccess(List<SuccessReusable> onSuccess) {
        this.onSuccess = onSuccess;
    }

    public List<FailureReusable> getOnFailure() {
        return onFailure;
    }

    public void setOnFailure(List<FailureReusable> onFailure) {
        this.onFailure = onFailure;
    }
}
