package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Info related to CQL command execution
 */
public class ExecutedCqlCommand implements Serializable {

    /**
     * A constant to represent that execution time could not be obtained.
     */
    public static final long FAILURE_EXECUTION_TIME = -1L;

    /**
     * The actual CQL string with the command that was executed
     */
    private final String cqlCommand;

    /**
     * Whether the CQL command failed, for any reason
     */
    private final boolean threwCqlException;

    /**
     * Execution time
     */
    private final long executionTime;

    public ExecutedCqlCommand(String cqlCommand, boolean threwCqlException, long executionTime) {
        this.cqlCommand = cqlCommand;
        this.threwCqlException = threwCqlException;
        this.executionTime = executionTime;
    }

    public String getCqlCommand() {
        return cqlCommand;
    }

    public boolean hasThrownCqlException() {
        return threwCqlException;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutedCqlCommand executedCqlCommand = (ExecutedCqlCommand) o;
        return threwCqlException == executedCqlCommand.threwCqlException && Objects.equals(cqlCommand, executedCqlCommand.cqlCommand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cqlCommand, threwCqlException);
    }
}
