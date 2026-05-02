package org.evomaster.arazzo.models.domain;

import org.evomaster.arazzo.resolver.ArazzoReferenceResolver;

import java.util.List;
import java.util.Map;

/**
 * Representing the model Step Object
 * Describes a single workflow step which MAY be a call
 * to an API operation (OpenAPI Operation Object) or another Workflow Object
 * This model only have SuccessAction, FailureAction and Parameter.
 * The references are expected to be resolved by {@link ArazzoReferenceResolver}.
 */
public class Step {
    private String description;
    private String stepId;
    private String operationId;
    private String operationPath;
    private String workflowId;
    private List<Parameter> parameters;
    private RequestBody requestBody;
    private List<Criterion> successCriteria;
    private List<SuccessAction> onSuccess;
    private List<FailureAction> onFailure;
    private Map<String, String> outputs;

    public Step(Builder builder) {
        this.description = builder.description;
        this.stepId = builder.stepId;
        this.operationId = builder.operationId;
        this.operationPath = builder.operationPath;
        this.workflowId = builder.workflowId;
        this.parameters = builder.parameters;
        this.requestBody = builder.requestBody;
        this.successCriteria = builder.successCriteria;
        this.onSuccess = builder.onSuccess;
        this.onFailure = builder.onFailure;
        this.outputs = builder.outputs;
    }

    public String getDescription() {
        return description;
    }

    public String getStepId() {
        return stepId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getOperationPath() {
        return operationPath;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public List<Criterion> getSuccessCriteria() {
        return successCriteria;
    }

    public List<SuccessAction> getOnSuccess() {
        return onSuccess;
    }

    public List<FailureAction> getOnFailure() {
        return onFailure;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String description;
        private String stepId;
        private String operationId;
        private String operationPath;
        private String workflowId;
        private List<Parameter> parameters;
        private RequestBody requestBody;
        private List<Criterion> successCriteria;
        private List<SuccessAction> onSuccess;
        private List<FailureAction> onFailure;
        private Map<String, String> outputs;

        public Builder description(String description) { this.description = description; return this; }
        public Builder stepId(String stepId) { this.stepId = stepId; return this; }
        public Builder operationId(String operationId) { this.operationId = operationId; return this; }
        public Builder operationPath(String operationPath) { this.operationPath = operationPath; return this; }
        public Builder workflowId(String workflowId) { this.workflowId = workflowId; return this; }
        public Builder parameters(List<Parameter> parameters) { this.parameters = parameters; return this; }
        public Builder requestBody(RequestBody requestBody) { this.requestBody = requestBody; return this; }
        public Builder successCriteria(List<Criterion> successCriteria) { this.successCriteria = successCriteria; return this; }
        public Builder onSuccess(List<SuccessAction> onSuccess) { this.onSuccess = onSuccess; return this; }
        public Builder onFailure(List<FailureAction> onFailure) { this.onFailure = onFailure; return this; }
        public Builder outputs(Map<String, String> outputs) { this.outputs = outputs; return this; }

        public Step build() {
            return new Step(this);
        }
    }
}