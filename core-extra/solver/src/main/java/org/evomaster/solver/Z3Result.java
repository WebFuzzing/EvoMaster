package org.evomaster.solver;

/**
 * Represents the outcome of a Z3 solver invocation.
 * Distinguishes between four possible states: SAT (satisfiable with a solution),
 * UNSAT (unsatisfiable), UNKNOWN (Z3 could not decide, e.g. an incomplete theory
 * or a timeout), and ERROR (solver or parsing failure).
 */
public class Z3Result {

    public enum Status {
        /** Z3 found a satisfying assignment. The solution is available. */
        SAT,
        /** The problem is unsatisfiable. No solution exists. */
        UNSAT,
        /**
         * Z3 returned {@code unknown}: it could neither prove SAT nor UNSAT.
         * Typical causes are an incomplete theory (e.g. non-linear arithmetic,
         * quantifiers, some string/regex constraints) or a timeout. This is distinct
         * from ERROR: the solver ran correctly, it just could not decide.
         */
        UNKNOWN,
        /** A solver, I/O, or parsing error occurred. */
        ERROR
    }

    private final Status status;
    /** Non-null only when status == SAT. */
    private final Z3Solution solution;
    /** Non-null only when status == ERROR. */
    private final String errorMessage;

    private Z3Result(Status status, Z3Solution solution, String errorMessage) {
        this.status = status;
        this.solution = solution;
        this.errorMessage = errorMessage;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * @return the satisfying solution; non-null only when {@link #getStatus()} is SAT.
     */
    public Z3Solution getSolution() {
        return solution;
    }

    /**
     * @return the error message; non-null only when {@link #getStatus()} is ERROR.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public static Z3Result sat(Z3Solution solution) {
        return new Z3Result(Status.SAT, solution, null);
    }

    public static Z3Result unsat() {
        return new Z3Result(Status.UNSAT, null, null);
    }

    public static Z3Result unknown() {
        return new Z3Result(Status.UNKNOWN, null, null);
    }

    public static Z3Result error(String message) {
        return new Z3Result(Status.ERROR, null, message);
    }
}
