package com.webfuzzing.arazzo.models.domain;

/**
 * It represents an object that can be a {@link SuccessAction} or a {@link Reusable}.
 */
public abstract class SuccessReusable {

    public SuccessReusable() {
    }

    public static class Success extends SuccessReusable {
        private final SuccessAction action;

        public Success(SuccessAction action) {
            this.action = action;
        }

        public SuccessAction getAction() {
            return action;
        }
    }

    public static class ReusableObj extends SuccessReusable {
        private final Reusable reusable;

        public ReusableObj(Reusable reusable) {
            this.reusable = reusable;
        }

        public Reusable getReusable() {
            return reusable;
        }
    }
}
