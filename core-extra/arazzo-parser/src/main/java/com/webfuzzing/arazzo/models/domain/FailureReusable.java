package com.webfuzzing.arazzo.models.domain;

/**
 * It represents an object that can be a {@link FailureAction} or a {@link Reusable}.
 */
public abstract class FailureReusable {

    public FailureReusable() {
    }

    public static class Failure extends FailureReusable {
        private final FailureAction action;

        public Failure(FailureAction action) {
            this.action = action;
        }

        public FailureAction getAction() {
            return action;
        }
    }

    public static class ReusableObj extends FailureReusable {
        private final Reusable reusable;

        public ReusableObj(Reusable reusable) {
            this.reusable = reusable;
        }

        public Reusable getReusable() {
            return reusable;
        }
    }
}
