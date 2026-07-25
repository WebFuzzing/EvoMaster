package org.evomaster.solver.smtlib;

/**
 * The textual response tokens produced by SMT-LIB's {@code (check-sat)} command, as returned by Z3.
 * Shared between {@code Z3DockerExecutor} (which classifies the raw output) and {@link SMTResultParser}
 * so the tokens are defined in a single place.
 */
public final class CheckSatResponse {

    private CheckSatResponse() {
    }

    public static final String SAT = "sat";
    public static final String UNSAT = "unsat";
    public static final String UNKNOWN = "unknown";
}
