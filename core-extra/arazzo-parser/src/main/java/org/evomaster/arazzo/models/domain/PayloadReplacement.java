package org.evomaster.arazzo.models.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.evomaster.arazzo.deserializer.AnyExpressionDeserializer;

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
