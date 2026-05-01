package org.evomaster.arazzo.models.domain;

import java.util.Map;
import io.swagger.v3.oas.models.media.Schema;

public class Components {
    private Map<String, Schema<?>> inputs;
    private Map<String, Parameter> parameters;
    private Map<String, SuccessAction> successAction;
    private Map<String, FailureAction> failureAction;

    public Components() {
    }

    public Map<String, Schema<?>> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Schema<?>> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public Map<String, SuccessAction> getSuccessAction() {
        return successAction;
    }

    public void setSuccessAction(Map<String, SuccessAction> successAction) {
        this.successAction = successAction;
    }

    public Map<String, FailureAction> getFailureAction() {
        return failureAction;
    }

    public void setFailureAction(Map<String, FailureAction> failureAction) {
        this.failureAction = failureAction;
    }
}
