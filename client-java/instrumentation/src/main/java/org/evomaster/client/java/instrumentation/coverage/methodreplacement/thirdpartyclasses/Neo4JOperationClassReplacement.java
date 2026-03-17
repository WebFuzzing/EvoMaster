package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.Neo4JRunCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

/**
 * Base class for Neo4j driver method replacements.
 * Provides common functionality for tracking Neo4j database operations.
 */
public abstract class Neo4JOperationClassReplacement extends ThirdPartyMethodReplacementClass {

    /**
     * Tracks a Neo4j database operation by recording the query, parameters, execution time, and success status.
     *
     * @param query the Cypher query string
     * @param parameters the query parameters
     * @param successfullyExecuted whether the query executed successfully
     * @param executionTime the execution time in milliseconds
     */
    protected static void handleNeo4J(String query, Object parameters, boolean successfullyExecuted, long executionTime) {
        Neo4JRunCommand info = new Neo4JRunCommand(query, parameters, successfullyExecuted, executionTime);
        ExecutionTracer.addNeo4JInfo(info);
    }

}
