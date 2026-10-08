package com.webfuzzing.arazzo.models.domain;

/**
 * It represents an object that can be a {@link Parameter} or a {@link Reusable}.
 */
public abstract class ParameterReusable {

    public ParameterReusable() {
    }

    public static class Param extends ParameterReusable {
        private final Parameter parameter;

        public Param(Parameter parameter) {
            this.parameter = parameter;
        }

        public Parameter getParameter() {
            return parameter;
        }
    }

    public static class ReusableObj extends ParameterReusable {
        private final Reusable reusable;

        public ReusableObj(Reusable reusable) {
            this.reusable = reusable;
        }

        public Reusable getReusable() {
            return reusable;
        }
    }
}
