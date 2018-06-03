package org.evomaster.clientJava.controllerApi.dto.database.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Each time a SQL Select is executed, we keep track of which tables
 * and columns are involved.
 * Not only in the retrieved data, but anywhere in the query (eg WHERE clauses).
 *
 */
public class ReadDbDataDto {

    /**
     * Map from Table Name to Column Names.
     * The value "*" means all columns in the table.
     */
    public Map<String, Set<String>> queriedData = new HashMap<>();
}
