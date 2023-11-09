package org.evomaster.client.java.controller.api.dto.database.execution;

import java.util.*;


/**
 * Each time a SQL command is executed, we keep track of which tables
 * and columns are involved.
 *
 * All the following Maps are in the form:
 * from Table Name (key) to Column Name sets (value).
 * The value "*" means all columns in the table.
 *
 * Note: we keep track of what the SUT tried to execute on the database, but
 * not the result, eg, a DELETE might have deleted nothing if its WHERE
 * clause was not satisfied.
 */
public class ExecutionDto {

    /**
     * What was tried to be retrieved in a SELECT.
     * Something like "select x from Foo" would give info on "Foo-&gt;{x}".
     *
     * However, at times, what is returned is not directly the content of a column, but
     * rather some computations on it.
     * For example, in "select avg(x) from Foo", we would still be just interested in
     * the info that the data in "Foo-&gt;{x}" was used to compute the result.
     *
     * Note: this does NOT include what was used in the WHERE clauses.
     * For example, "select a,b from Foo where c&gt;0" would give info just for "Foo-&gt;{a,b}"
     */
    public Map<String, Set<String>> queriedData = new HashMap<>();


    /**
     * What tables/columns were tried to be updated in UPDATE
     */
    public Map<String, Set<String>> updatedData = new HashMap<>();


    /**
     * What tables/columns were tried to be newly created with INSERT
     */
    public Map<String, Set<String>> insertedData = new HashMap<>();


    /**
     * Names of tables on which DELETE was applied
     */
    public Set<String> deletedData = new HashSet<>();


    /**
     * Every time there is a WHERE clause which "failed" (ie, resulted in false),
     * we keep track of which tables/columns where involved in determining the
     * result of the WHERE.
     *
     * If there is no WHERE, but still no data was returned, we consider it
     * as a failed WHERE
     */
    public Map<String, Set<String>> failedWhere = new HashMap<>();

    /**
     * The total Number of SQL commands (e.g., SELECT and UPDATE) executed
     */
    public int numberOfSqlCommands = 0;

    /**
     * log of a list of sql commands that were executed
     */
    public List<SqlExecutionLogDto> sqlExecutionLogDtoList = new ArrayList<>();
}
