package org.evomaster.client.java.controller.api.dto.database.execution;


import java.util.ArrayList;
import java.util.List;

/**
 * Reports Cassandra query executions that are useful as data-generation hints, e.g. queries
 * whose target table came back empty.
 */
public class CassandraExecutionsDto {
    public List<CassandraFailedQuery> failedQueries = new ArrayList<>();
}