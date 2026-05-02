package org.evomaster.arazzo.models.domain;

import java.util.List;

/**
 * Representing the model Success Action Object
 * A single success action which describes an action to take upon success of a workflow step
 */
public class SuccessAction {
    private String name;
    private String type;
    private String workflowId;
    private String stepId;
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
