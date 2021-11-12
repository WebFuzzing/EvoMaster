package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Info related to SQL command execution.
 *
 * This class is IMMUTABLE
 */
public class SqlInfo implements Serializable {


    public static transient long FAILURE_EXTIME = -1L;
    /**
     * The actual SQL string with the command that was executed
     */
    private final String command;

    /**
     * Whether the command led to any result:
     * eg if there was any data returned in SELECT or any rows
     * modified in a UPDATE or data added with INSERT.
     * Failure here usually/often means that the predicates on WHERE and
     * ON clauses were not satisfied
     */
    private final boolean noResult;

    /**
     * Whether the SQL command failed, for any reason
     */
    private final boolean exception;

    /**
     * execution time
     */
    private final long executionTime;


    public SqlInfo(String command, boolean noResult, boolean exception) {
        this(command, noResult, exception, FAILURE_EXTIME);
    }

    public SqlInfo(String command, boolean noResult, boolean exception, long executionTime) {
        this.command = command;
        this.noResult = noResult;
        this.exception = exception;
        this.executionTime = executionTime;
    }

    public String getCommand() {
        return command;
    }

    public boolean isNoResult() {
        return noResult;
    }

    public boolean isException() {
        return exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlInfo sqlInfo = (SqlInfo) o;
        return noResult == sqlInfo.noResult && exception == sqlInfo.exception && Objects.equals(command, sqlInfo.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, noResult, exception);
    }

    public long getExecutionTime() {
        return executionTime;
    }

}
