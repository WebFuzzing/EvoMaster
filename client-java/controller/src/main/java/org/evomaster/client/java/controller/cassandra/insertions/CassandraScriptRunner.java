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
     * <p>
     * A failing insertion does not stop the ones that follow it: it is just marked as failed in the
     * returned results, and the execution carries on with the next insertion.
     *
     * @param connection a connection to the database (CqlSession)
     * @param insertions the Cassandra insertions to execute
     * @return a CassandraInsertionResultsDto stating, for each insertion, whether it executed successfully
     * @throws IllegalArgumentException if there is no insertion to execute
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
                SimpleLogger.debug("Insertion executed successfully");
            } catch (Exception e) {
                /*
                    Cassandra has no foreign keys nor referential integrity between rows, so a
                    failed insertion does not invalidate the ones that follow it. As such, the index is just
                    marked as failed and the execution carries on, in the same way as done in
                    SqlScriptRunner#execInsert.
                 */
                String msg = "Failed to execute insertion";
                SimpleLogger.warn(msg, extractError(e));
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

    /**
     * As the CQL statements are executed via reflection, the actual error thrown by the driver comes
     * wrapped into an {@link InvocationTargetException}, and so it needs to be unwrapped to be reported.
     */
    private static Throwable extractError(Exception e) {
        if (e instanceof InvocationTargetException) {
            return ((InvocationTargetException) e).getTargetException();
        }
        return e;
    }
}
