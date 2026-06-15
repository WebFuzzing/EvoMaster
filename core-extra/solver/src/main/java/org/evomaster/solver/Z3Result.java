package org.evomaster.solver;

import org.evomaster.solver.smtlib.value.SMTLibValue;

import java.util.Map;

/**
 * Represents the outcome of a Z3 solver invocation.
 * Distinguishes between three possible states: SAT (satisfiable with a model),
 * UNSAT (unsatisfiable), and ERROR (solver or parsing failure).
 */
public class Z3Result {

    public enum Status {
        /** Z3 found a satisfying assignment. The model is available. */
        SAT,
        /** The problem is unsatisfiable. No model exists. */
        UNSAT,
        /** A solver, I/O, or parsing error occurred. */
        ERROR
    }

    public final Status status;
    /** Non-null only when status == SAT. */
    public final Map<String, SMTLibValue> model;
    /** Non-null only when status == ERROR. */
    public final String errorMessage;

    private Z3Result(Status status, Map<String, SMTLibValue> model, String errorMessage) {
        this.status = status;
        this.model = model;
        this.errorMessage = errorMessage;
    }

    public static Z3Result sat(Map<String, SMTLibValue> model) {
        return new Z3Result(Status.SAT, model, null);
    }

    public static Z3Result unsat() {
        return new Z3Result(Status.UNSAT, null, null);
    }

    public static Z3Result error(String message) {
        return new Z3Result(Status.ERROR, null, message);
    }
}
