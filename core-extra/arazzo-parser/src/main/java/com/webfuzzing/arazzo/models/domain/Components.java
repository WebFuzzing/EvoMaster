package com.webfuzzing.arazzo.models.domain;

import java.util.Map;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Representing the model Components Object
 * Holds a set of reusable objects for different aspects of the Arazzo Specification.
 * All objects defined within the components object will have no effect on the Arazzo Description
 * unless they are explicitly referenced from properties outside the components object.
 */
public class Components {
    /**
     * An object to hold reusable JSON Schema objects to be referenced from workflow inputs.
     */
    private Map<String, Schema<?>> inputs;

    /**
     * An object to hold reusable Parameter Objects
     */
    private Map<String, Parameter> parameters;

    /**
     * An object to hold reusable Success Actions Objects.
     */
    private Map<String, SuccessAction> successAction;

    /**
     * An object to hold reusable Failure Actions Objects.
     */
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
