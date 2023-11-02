package org.evomaster.client.java.controller.api.dto.database.execution;

/**
 * sql execution info in detail
 */
public class SqlExecutionLogDto {

    /**
     * sql string to be executed
     */
    public String command;

    /**
     * time spent by executing the command
     */
    public long executionTime;

    public SqlExecutionLogDto(){}

    public SqlExecutionLogDto(String command, long executionTime) {
        this.command = command;
        this.executionTime = executionTime;
    }
}
