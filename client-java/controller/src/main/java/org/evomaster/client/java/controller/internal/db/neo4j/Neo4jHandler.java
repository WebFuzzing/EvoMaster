package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jFailedQuery;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionKeyBuilder;
import org.evomaster.client.java.controller.neo4j.Neo4jInsertionTemplateBuilder;
import org.evomaster.client.java.controller.neo4j.ReflectionBasedNeo4jClient;
import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.heuristics.Neo4jHeuristicsCalculator;
import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;
import org.evomaster.client.java.controller.neo4j.parser.CypherParser;
import org.evomaster.client.java.controller.neo4j.parser.CypherParserException;
import org.evomaster.client.java.controller.neo4j.parser.CypherParserFactory;
import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.instrumentation.Neo4JRunCommand;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Acts upon Cypher queries executed by the SUT (captured as {@link Neo4JRunCommand}s): for each
 * captured query it computes how close the live graph is to satisfying it, as a distance to minimize.
 * Only MATCH queries are scored; a query that does not parse as a MATCH (e.g. a write) is skipped.
 * A query the graph did not satisfy is also digested into the insertion that would satisfy it, for
 * the core to generate test data from.
 */
public class Neo4jHandler {

    /** Cypher queries captured from {@code Session.run}, pending evaluation. */
    private final List<Neo4JRunCommand> operations;

    /** The computed heuristics, one per scored query. */
    private final List<Neo4jCommandWithDistance> commandsWithDistances;

    /** Whether to compute heuristics based on execution or not. */
    private volatile boolean calculateHeuristics;

    /** Whether to collect the queries the graph did not satisfy, for test-data generation. */
    private volatile boolean extractNeo4jExecution;

    /** The queries of the current action that the graph did not satisfy, each digested into an insertion. */
    private final List<Neo4jFailedQuery> failedQueries = new ArrayList<>();

    /** The keys of the insertions in {@link #failedQueries}, so that one is registered only once per action. */
    private final Set<String> insertionKeys = new LinkedHashSet<>();

    /** Client over the SUT's Neo4j driver. {@code null} when the SUT does not use Neo4j. */
    private ReflectionBasedNeo4jClient neo4jConnection = null;

    private final CypherParser parser = CypherParserFactory.buildParser();

    private final Neo4jHeuristicsCalculator calculator =
            new Neo4jHeuristicsCalculator(new TaintHandlerExecutionTracer());
    private final Neo4jGraphReader graphReader = new Neo4jGraphReader();

    /**
     * Creates a handler with heuristic calculation enabled.
     */
    public Neo4jHandler() {
        operations = new ArrayList<>();
        commandsWithDistances = new ArrayList<>();
        calculateHeuristics = true;
        extractNeo4jExecution = true;
    }

    /**
     * Clears data collected for the current action.
     */
    public void reset() {
        operations.clear();
        commandsWithDistances.clear();
        failedQueries.clear();
        insertionKeys.clear();
    }

    /**
     * @return whether Neo4j heuristic calculation is enabled
     */
    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    /**
     * Enables or disables Neo4j heuristic calculation.
     *
     * @param calculateHeuristics new calculation state
     */
    public void setCalculateHeuristics(boolean calculateHeuristics) {
        this.calculateHeuristics = calculateHeuristics;
    }

    /**
     * @return whether the queries the graph did not satisfy are collected
     */
    public boolean isExtractNeo4jExecution() {
        return extractNeo4jExecution;
    }

    /**
     * Enables or disables collecting the queries the graph did not satisfy.
     *
     * @param extractNeo4jExecution new extraction state
     */
    public void setExtractNeo4jExecution(boolean extractNeo4jExecution) {
        this.extractNeo4jExecution = extractNeo4jExecution;
    }

    /**
     * @return the queries of the current action that the graph did not satisfy, as insertions
     */
    public Neo4jExecutionsDto getExecutionDto() {
        Neo4jExecutionsDto dto = new Neo4jExecutionsDto();
        dto.failedQueries.addAll(failedQueries);
        return dto;
    }

    /**
     * Sets the client used to read the live graph.
     *
     * @param neo4jConnection client over the SUT's Neo4j driver, or {@code null} if it has none
     */
    public void setNeo4jConnection(ReflectionBasedNeo4jClient neo4jConnection) {
        this.neo4jConnection = neo4jConnection;
    }

    /**
     * Registers one intercepted Cypher query.
     *
     * @param info intercepted query
     */
    public void handle(Neo4JRunCommand info) {
        if ((calculateHeuristics || extractNeo4jExecution) && info.getQuery() != null) {
            operations.add(info);
        }
    }

    /**
     * Evaluates all registered queries against a single snapshot of the graph, and consumes them.
     * The snapshot is read once per action rather than per query, since the SUT is not running while
     * the heuristics are computed.
     *
     * @return evaluated queries for the current action
     */
    public List<Neo4jCommandWithDistance> getEvaluatedNeo4jCommands() {

        if ((!calculateHeuristics && !extractNeo4jExecution) || neo4jConnection == null || operations.isEmpty()) {
            operations.clear();
            return commandsWithDistances;
        }

        Neo4jGraph graph;
        try {
            graph = graphReader.read(neo4jConnection);
        } catch (Exception e) {
            SimpleLogger.uniqueWarn("Failed to read the Neo4j graph to compute heuristics: " + e.getMessage());
            operations.clear();
            return commandsWithDistances;
        }

        for (Neo4JRunCommand op : operations) {
            String query = op.getQuery();
            final MatchOperation parsedQuery;
            try {
                parsedQuery = parser.parse(query);
            } catch (CypherParserException e) {
                SimpleLogger.uniqueWarn("Failed to parse Cypher query for Neo4j heuristics: " + e.getMessage());
                continue;
            }

            Map<String, Object> parameters;
            try {
                parameters = ReflectionBasedNeo4jClient.parametersAsMap(op.getParameters());
            } catch (Exception e) {
                SimpleLogger.uniqueWarn("Failed to read the parameters of a Cypher query for Neo4j heuristics: "
                        + e.getMessage());
                parameters = Collections.emptyMap();
            }

            Neo4jDistanceWithMetrics metrics;
            try {
                double distance = calculator.computeDistance(parsedQuery, graph, parameters);
                metrics = new Neo4jDistanceWithMetrics(distance, graph.nodeCount(), false);
                if (extractNeo4jExecution && distance > 0.0d) {
                    registerFailedQuery(parsedQuery, parameters, query);
                }
            } catch (Exception e) {
                SimpleLogger.uniqueWarn("Failed to compute Neo4j heuristic for query: " + query
                        + " | cause: " + e.getClass().getName() + ": " + e.getMessage());
                metrics = new Neo4jDistanceWithMetrics(Neo4jHeuristicsCalculator.MAX_NEO4J_DISTANCE, graph.nodeCount(), true);
            }
            if (calculateHeuristics) {
                commandsWithDistances.add(new Neo4jCommandWithDistance(query, metrics));
            }
        }

        operations.clear();
        return commandsWithDistances;
    }

    /**
     * Keeps the insertion that would satisfy a query the graph did not, once per distinct insertion.
     */
    private void registerFailedQuery(MatchOperation parsedQuery, Map<String, Object> parameters, String query) {
        Neo4jFailedQuery failed = Neo4jInsertionTemplateBuilder.build(parsedQuery, parameters, query);
        if (failed == null) {
            SimpleLogger.uniqueWarn("Cannot derive the data to insert from Cypher query: " + query);
            return;
        }
        if (insertionKeys.add(Neo4jInsertionKeyBuilder.fromCommands(failed.nodes, failed.edges))) {
            failedQueries.add(failed);
        }
    }
}
