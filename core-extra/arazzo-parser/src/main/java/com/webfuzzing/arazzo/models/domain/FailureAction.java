package com.webfuzzing.arazzo.models.domain;

import java.util.List;

/**
 * Representing the model Failure Action Object
 * A single failure action which describes an action to take upon failure of a workflow step
 */
public class FailureAction {

    /**
     * The name of the failure action.
     */
    private String name;

    /**
     * The type of action to take.
     */
    private String type;

    /**
     * The workflowId referencing an existing workflow within the Arazzo Description to transfer to upon failure of the step.
     */
    private String workflowId;

    /**
     * The stepId to transfer to upon failure of the step.
     */
    private String stepId;

    /**
     * A non-negative decimal indicating the seconds to delay after the step failure before another attempt SHALL be made.
     */
    private Number retryAfter;

    /**
     * A non-negative integer indicating how many attempts to retry the step MAY be attempted before failing the overall step.
     */
    private Integer retryLimit;

    /**
     * A list of assertions to determine if this action SHALL be executed.
     */
    private List<Criterion> criteria;

    public FailureAction() {
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

    public Number getRetryAfter() {
        return retryAfter;
    }

    public void setRetryAfter(Number retryAfter) {
        this.retryAfter = retryAfter;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public Integer getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(Integer retryLimit) {
        this.retryLimit = retryLimit;
    }

    public List<Criterion> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<Criterion> criteria) {
        this.criteria = criteria;
    }
}
