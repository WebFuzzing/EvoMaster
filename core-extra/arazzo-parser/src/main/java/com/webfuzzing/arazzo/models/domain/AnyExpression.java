package com.webfuzzing.arazzo.models.domain;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Representing the model (Any | {expression})
 * The value to pass in the parameter.
 * The value can be a constant or a Runtime Expression to be evaluated
 * and passed to the referenced operation or workflow
 */
public abstract class AnyExpression {

    public AnyExpression() {
    }

    public abstract String asString();

    public static class Any extends AnyExpression {
        private final JsonNode jsonNode;

        public Any(JsonNode jsonNode) {
            this.jsonNode = jsonNode;
        }

        public JsonNode getJsonNode() {
            return jsonNode;
        }

        @Override
        public String asString() {
            if (jsonNode == null || jsonNode.isNull()) {
                return null;
            }
            if (jsonNode.isValueNode()) {
                return jsonNode.asText();
            }
            return jsonNode.toString();
        }
    }

    public static class Expression extends AnyExpression {
        private final String expression;

        public Expression(String expression) {
            this.expression = expression;
        }

        public String getExpression() {
            return expression;
        }

        @Override
        public String asString() {
            return expression;
        }
    }
}
