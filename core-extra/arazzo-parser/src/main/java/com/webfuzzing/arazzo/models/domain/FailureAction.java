package com.webfuzzing.arazzo.models.domain;

import java.util.List;

/**
 * Representing the model Failure Action Object
 * A single failure action which describes an action to take upon failure of a workflow step
 */
public class FailureAction {
    private String name;
    private String type;
    private String workflowId;
    private String stepId;
    private Number retryAfter;
    private Integer retryLimit;
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
