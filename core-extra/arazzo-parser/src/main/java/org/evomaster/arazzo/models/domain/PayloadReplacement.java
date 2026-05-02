package org.evomaster.arazzo.models.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.evomaster.arazzo.deserializer.AnyExpressionDeserializer;

/**
 * Representing the model Payload Replacement Object
 * Describes a location within a payload (e.g., a request body) and a value to set within the location.
 */
public class PayloadReplacement {
    private String target;

    @JsonDeserialize(using = AnyExpressionDeserializer.class)
    private AnyExpression value;

    public PayloadReplacement() {
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public AnyExpression getValue() {
        return value;
    }

    public void setValue(AnyExpression value) {
        this.value = value;
    }
}
