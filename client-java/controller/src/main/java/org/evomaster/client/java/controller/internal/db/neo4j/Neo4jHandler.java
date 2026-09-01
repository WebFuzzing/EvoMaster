package org.evomaster.client.java.controller.internal.db.neo4j;

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
import java.util.List;

/**
 * Acts upon Cypher queries executed by the SUT (captured as {@link Neo4JRunCommand}s): for each
 * captured query it computes how close the live graph is to satisfying it, as a distance to minimize.
 * Only MATCH queries are scored; a query that does not parse as a MATCH (e.g. a write) is skipped.
 */
public class Neo4jHandler {

    /** Cypher queries captured from {@code Session.run}, pending evaluation. */
    private final List<Neo4JRunCommand> operations;

    /** The computed heuristics, one per scored query. */
    private final List<Neo4jCommandWithDistance> commandsWithDistances;

    /** Whether to compute heuristics based on execution or not. */
    private volatile boolean calculateHeuristics;

    /**
     * The SUT's {@code org.neo4j.driver.Driver}, kept as an {@code Object} and used by reflection so
     * we do not hard-depend on a specific driver version. {@code null} when the SUT does not use Neo4j.
     */
    private Object neo4jConnection = null;

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
    }

    /**
     * Clears data collected for the current action.
     */
    public void reset() {
        operations.clear();
        commandsWithDistances.clear();
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
     * Sets the driver used to read the live graph.
     *
     * @param neo4jConnection the SUT's {@code org.neo4j.driver.Driver}, or {@code null} if it has none
     */
    public void setNeo4jConnection(Object neo4jConnection) {
        this.neo4jConnection = neo4jConnection;
    }

    /**
     * Registers one intercepted Cypher query.
     *
     * @param info intercepted query
     */
    public void handle(Neo4JRunCommand info) {
        if (calculateHeuristics && info.getQuery() != null) {
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

        if (!calculateHeuristics || neo4jConnection == null || operations.isEmpty()) {
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

            Neo4jDistanceWithMetrics metrics;
            try {
                double distance = calculator.computeDistance(parsedQuery, graph);
                metrics = new Neo4jDistanceWithMetrics(distance, graph.nodeCount(), false);
            } catch (Exception e) {
                SimpleLogger.uniqueWarn("Failed to compute Neo4j heuristic for query: " + query);
                metrics = new Neo4jDistanceWithMetrics(1.0, graph.nodeCount(), true);
            }
            commandsWithDistances.add(new Neo4jCommandWithDistance(query, metrics));
        }

        operations.clear();
        return commandsWithDistances;
    }
}
