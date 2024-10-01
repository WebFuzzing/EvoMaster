package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Info related to SQL command execution.
 *
 * This class is IMMUTABLE
 */
public class ExecutedSqlCommand implements Serializable {


    /**
     * A constant to represent that execution time could not be obtained.
     */
    public static final long FAILURE_EXECUTION_TIME = -1L;

    /**
     * The actual SQL string with the command that was executed
     */
    private final String sqlCommand;

    /**
     * Whether the SQL command failed, for any reason
     */
    private final boolean threwSqlException;

    /**
     * execution time
     */
    private final long executionTime;


    public ExecutedSqlCommand(String sqlCommand, boolean threwSqlException, long executionTime) {
        this.sqlCommand = sqlCommand;
        this.threwSqlException = threwSqlException;
        this.executionTime = executionTime;
    }

    public String getSqlCommand() {
        return sqlCommand;
    }

    public boolean hasThrownSqlException() {
        return threwSqlException;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutedSqlCommand executedSqlCommand = (ExecutedSqlCommand) o;
        return threwSqlException == executedSqlCommand.threwSqlException && Objects.equals(sqlCommand, executedSqlCommand.sqlCommand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlCommand, threwSqlException);
    }

    public long getExecutionTime() {
        return executionTime;
    }

}
