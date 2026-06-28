package com.webfuzzing.arazzo.models.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.webfuzzing.arazzo.deserializer.CriterionTypeDeserializer;

/**
 * Representing the model Criterion Object
 * An object used to specify the context, conditions,
 * and condition types that can be used to prove or satisfy assertions specified
 */
public class Criterion {
    /**
     * A Runtime Expression used to set the context for the condition to be applied on.
     */
    private String context;

    /**
     * The condition to apply.
     */
    private String condition;

    /**
     * The type of condition to be applied.
     */
    @JsonDeserialize(using = CriterionTypeDeserializer.class)
    private CriterionType type;

    public Criterion() {
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public CriterionType getType() {
        return type;
    }

    public void setType(CriterionType type) {
        this.type = type;
    }
}
