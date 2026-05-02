package org.evomaster.arazzo.models.domain;

import io.swagger.v3.oas.models.media.Schema;
import org.evomaster.arazzo.resolver.ArazzoReferenceResolver;

import java.util.List;
import java.util.Map;

/**
 * Representing the model Workflow Object
 * Describes the steps to be taken across one or more APIs to achieve an objective.
 * The workflow object MAY define inputs needed in order to execute workflow steps,
 * where the defined steps represent a call to an API operation or another workflow,
 * and a set of outputs.
 * This model only have SuccessAction, FailureAction and Parameter.
 * The references are expected to be resolved by {@link ArazzoReferenceResolver}.
 */
public class Workflow {
    private String workflowId;
    private String summary;
    private String description;
    private Schema<?> inputs;
    private List<String> dependsOn;
    private List<Step> steps;
    private List<SuccessAction> successActions;
    private List<FailureAction> failureActions;
    private Map<String, String> outputs;
    private List<Parameter> parameters;

    private Workflow(Builder builder) {
        this.workflowId = builder.workflowId;
        this.summary = builder.summary;
        this.description = builder.description;
        this.inputs = builder.inputs;
        this.dependsOn = builder.dependsOn;
        this.steps = builder.steps;
        this.successActions = builder.successActions;
        this.failureActions = builder.failureActions;
        this.outputs = builder.outputs;
        this.parameters = builder.parameters;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public Schema<?> getInputs() {
        return inputs;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public List<SuccessAction> getSuccessActions() {
        return successActions;
    }

    public List<FailureAction> getFailureActions() {
        return failureActions;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String workflowId;
        private String summary;
        private String description;
        private Schema<?> inputs;
        private List<String> dependsOn;
        private List<Step> steps;
        private List<SuccessAction> successActions;
        private List<FailureAction> failureActions;
        private Map<String, String> outputs;
        private List<Parameter> parameters;

        public Builder workflowId(String workflowId) { this.workflowId = workflowId; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder inputs(Schema<?> inputs) { this.inputs = inputs; return this; }
        public Builder dependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; return this; }
        public Builder steps(List<Step> steps) { this.steps = steps; return this; }
        public Builder successActions(List<SuccessAction> successActions) { this.successActions = successActions; return this; }
        public Builder failureActions(List<FailureAction> failureActions) { this.failureActions = failureActions; return this; }
        public Builder outputs(Map<String, String> outputs) { this.outputs = outputs; return this; }
        public Builder parameters(List<Parameter> parameters) { this.parameters = parameters; return this; }

        public Workflow build() {
            return new Workflow(this);
        }
    }

}
