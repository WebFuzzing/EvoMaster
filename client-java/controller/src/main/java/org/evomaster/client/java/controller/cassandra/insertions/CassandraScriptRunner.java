package org.evomaster.client.java.controller.cassandra.insertions;

import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionResultsDto;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class used to execute Cassandra insertions built with {@link org.evomaster.client.java.controller.cassandra.dsl.CassandraDsl}.
 * The connection is driven purely via reflection, as the actual CqlSession instance handed in at runtime
 * comes from the SUT's own classpath/classloader (mirrors how MongoScriptRunner handles its connection).
 */
public class CassandraScriptRunner {

    /**
     * Default constructor
     */
    public CassandraScriptRunner() {
    }

    /**
     * Execute the different Cassandra insertions.
     *
     * @param connection a connection to the database (CqlSession)
     * @param insertions the Cassandra insertions to execute
     * @return a CassandraInsertionResultsDto
     */
    public static CassandraInsertionResultsDto executeInsert(Object connection, List<CassandraInsertionDto> insertions) {

        if (insertions == null || insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        List<Boolean> cassandraResults = new ArrayList<>(Collections.nCopies(insertions.size(), false));

        for (int i = 0; i < insertions.size(); i++) {

            CassandraInsertionDto insertionDto = insertions.get(i);

            try {
                String cql = prepareInsertCommand(insertionDto);
                executeCql(connection, cql);
                cassandraResults.set(i, true);
                SimpleLogger.debug(cql + " executed on keyspace: " + insertionDto.keyspaceName + " and table: " + insertionDto.tableName);
            } catch (Exception e) {
                final String errorMessage;
                if (e instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) e;
                    Throwable innerException = invocationTargetException.getTargetException();
                    errorMessage = innerException.getMessage();
                } else {
                    errorMessage = e.getMessage();
                }
                String msg = "Failed to execute insertion with index " + i + " with Cassandra. Error: " + errorMessage;
                throw new RuntimeException(msg, e);
            }
        }

        CassandraInsertionResultsDto insertionResultsDto = new CassandraInsertionResultsDto();
        insertionResultsDto.executionResults = cassandraResults;
        return insertionResultsDto;
    }

    private static String prepareInsertCommand(CassandraInsertionDto insDto) {

        StringBuilder cql = new StringBuilder("INSERT INTO ");
        cql.append(insDto.keyspaceName).append(".").append(insDto.tableName).append(" (");

        cql.append(insDto.data.stream()
                .map(e -> e.columnName)
                .collect(Collectors.joining(",")));

        cql.append(") VALUES (");

        cql.append(insDto.data.stream()
                .map(e -> e.printableValue)
                .collect(Collectors.joining(",")));

        cql.append(")");

        return cql.toString();
    }

    private static void executeCql(Object connection, String cql) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        connection.getClass().getMethod("execute", String.class).invoke(connection, cql);
    }
}
