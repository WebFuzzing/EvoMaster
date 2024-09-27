package org.evomaster.client.java.controller.api.dto.database.execution;

/**
 * sql execution info in detail
 */
public class SqlExecutionLogDto {

    /**
     * sql string monitored during instrumentation
     */
    public String sqlCommand;

    /**
     * time spent by executing the SQL command
     */
    public long executionTime;

    /**
     * Indicates whether an SQL exception has been thrown during execution.
     */
    public boolean threwSqlExeception;

    public SqlExecutionLogDto() {
    }

    public SqlExecutionLogDto(String sqlCommand, boolean threwSqlExeception, long executionTime) {
        this.sqlCommand = sqlCommand;
        this.executionTime = executionTime;
        this.threwSqlExeception = threwSqlExeception;
    }
}
