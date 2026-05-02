package org.evomaster.arazzo.models.domain;

/**
 * It represents an object that can be a String or a {@link CriterionExpression}.
 */
public abstract class CriterionType {

    public CriterionType() {
    }

    public static class Simple extends CriterionType {
        private final String value;

        public Simple(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class Complex extends CriterionType {
        private final CriterionExpression expression;

        public Complex(CriterionExpression expression) {
            this.expression = expression;
        }

        public CriterionExpression getExpression() {
            return expression;
        }
    }
}
