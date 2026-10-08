package com.webfuzzing.arazzo.models.domain;

import java.util.List;

/**
 * Representing the model Success Action Object
 * A single success action which describes an action to take upon success of a workflow step
 */
public class SuccessAction {
    /**
     * The name of the success action.
     */
    private String name;

    /**
     * The type of action to take.
     */
    private String type;

    /**
     * The workflowId referencing an existing workflow within the Arazzo Description to transfer to upon success of the step.
     */
    private String workflowId;

    /**
     * The stepId to transfer to upon success of the step.
     */
    private String stepId;

    /**
     * A list of assertions to determine if this action SHALL be executed.
     */
    private List<Criterion> criteria;

    public SuccessAction() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public List<Criterion> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<Criterion> criteria) {
        this.criteria = criteria;
    }
}
