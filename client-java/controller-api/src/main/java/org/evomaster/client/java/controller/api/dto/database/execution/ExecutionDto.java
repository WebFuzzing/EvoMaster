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
 * Note: we keep track of the SUT tried to execute on the database, but
 * not the result, eg, a DELETE might have deleted nothing if its WHERE
 * clause was not satisfied.
 */
public class ExecutionDto {

    /**
     * What was tried to be retrieved in a SELECT
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
    public List<String> deletedData = new ArrayList<>();


    /**
     * Every time there is a WHERE clause which "failed" (ie, resulted in false),
     * we keep track of which tables/columns where involved in determining the
     * result of the WHERE.
     */
    public Map<String, Set<String>> failedWhere = new HashMap<>();

}
