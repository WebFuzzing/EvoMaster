package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.Neo4JRunCommand;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.*;
import org.testcontainers.containers.GenericContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4JSessionClassReplacementTest {

    private static Driver driver;
    private static final int NEO4J_BOLT_PORT = 7687;

    private static final String PARAM_NAME = "name";
    private static final String FIELD_AGE = "age";
    private static final String MATCH_PERSON_BY_NAME_QUERY = "MATCH (p:Person {name: $name}) RETURN p.age AS age";

    private static final GenericContainer<?> neo4j = new GenericContainer<>("neo4j:5")
            .withExposedPorts(NEO4J_BOLT_PORT)
            .withEnv("NEO4J_AUTH", "none");

    @BeforeAll
    public static void initNeo4jDriver() {
        neo4j.start();

        String boltUrl = "bolt://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_BOLT_PORT);
        driver = GraphDatabase.driver(boltUrl, AuthTokens.none());
        driver.verifyConnectivity();

        ExecutionTracer.reset();
    }

    @AfterAll
    public static void closeDriver() {
        if (driver != null) {
            driver.close();
        }
        neo4j.stop();
        ExecutionTracer.reset();
    }

    @BeforeEach
    public void resetTracer() {
        // Clean up any existing data
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
        ExecutionTracer.reset();
    }

    @Test
    public void testRunWithStringQuery() {
        try (Session session = driver.session()) {
            String query = "RETURN 1 AS number";

            ExecutionTracer.setExecutingInitNeo4J(false);
            Result result = (Result) Neo4JSessionClassReplacement.run(session, query);

            assertTrue(result.hasNext());
            Record record = result.next();
            assertEquals(1, record.get("number").asInt());

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<Neo4JRunCommand> neo4jCommands = additionalInfoList.get(0).getNeo4JInfoData();
            assertEquals(1, neo4jCommands.size());

            Neo4JRunCommand command = neo4jCommands.iterator().next();
            assertEquals(query, command.getQuery());
            assertNull(command.getParameters());
            assertTrue(command.getSuccessfullyExecuted());
            assertTrue(command.getExecutionTime() >= 0);
        }
    }

    @Test
    public void testRunWithQueryObject() {
        try (Session session = driver.session()) {
            String queryText = "RETURN 'hello' AS greeting";
            Query query = new Query(queryText);

            ExecutionTracer.setExecutingInitNeo4J(false);
            Result result = (Result) Neo4JSessionClassReplacement.run(session, query);

            assertTrue(result.hasNext());
            Record record = result.next();
            assertEquals("hello", record.get("greeting").asString());

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<Neo4JRunCommand> neo4jCommands = additionalInfoList.get(0).getNeo4JInfoData();
            assertEquals(1, neo4jCommands.size());

            Neo4JRunCommand command = neo4jCommands.iterator().next();
            assertEquals(queryText, command.getQuery());
            Value capturedParams = (Value) command.getParameters();
            assertTrue(capturedParams.isEmpty());
            assertTrue(command.getSuccessfullyExecuted());
            assertTrue(command.getExecutionTime() >= 0);
        }
    }

    @Test
    public void testRunWithStringQueryAndMapParameters() {
        try (Session session = driver.session()) {
            String personName = "John";
            int personAge = 30;
            session.run("CREATE (p:Person {name: '" + personName + "', age: " + personAge + "})");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(PARAM_NAME, personName);

            ExecutionTracer.setExecutingInitNeo4J(false);
            Result result = (Result) Neo4JSessionClassReplacement.run(session, MATCH_PERSON_BY_NAME_QUERY, parameters);

            assertTrue(result.hasNext());
            Record record = result.next();
            assertEquals(personAge, record.get(FIELD_AGE).asInt());

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<Neo4JRunCommand> neo4jCommands = additionalInfoList.get(0).getNeo4JInfoData();
            assertEquals(1, neo4jCommands.size());

            Neo4JRunCommand command = neo4jCommands.iterator().next();
            assertEquals(MATCH_PERSON_BY_NAME_QUERY, command.getQuery());
            Map<String, Object> capturedParams = (Map<String, Object>) command.getParameters();
            assertEquals(1, capturedParams.size());
            assertEquals(personName, capturedParams.get(PARAM_NAME));
            assertTrue(command.getSuccessfullyExecuted());
            assertTrue(command.getExecutionTime() >= 0);
        }
    }

    @Test
    public void testRunWithQueryObjectAndParameters() {
        try (Session session = driver.session()) {
            String personName = "Jane";
            int personAge = 25;
            session.run("CREATE (p:Person {name: '" + personName + "', age: " + personAge + "})");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(PARAM_NAME, personName);
            Query query = new Query(MATCH_PERSON_BY_NAME_QUERY, parameters);

            ExecutionTracer.setExecutingInitNeo4J(false);
            Result result = (Result) Neo4JSessionClassReplacement.run(session, query);

            assertTrue(result.hasNext());
            Record record = result.next();
            assertEquals(personAge, record.get(FIELD_AGE).asInt());

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<Neo4JRunCommand> neo4jCommands = additionalInfoList.get(0).getNeo4JInfoData();
            assertEquals(1, neo4jCommands.size());

            Neo4JRunCommand command = neo4jCommands.iterator().next();
            assertEquals(MATCH_PERSON_BY_NAME_QUERY, command.getQuery());
            Value capturedParams = (Value) command.getParameters();
            assertEquals(1, capturedParams.size());
            assertEquals(personName, capturedParams.get(PARAM_NAME).asString());
            assertTrue(command.getSuccessfullyExecuted());
            assertTrue(command.getExecutionTime() >= 0);
        }
    }

    @Test
    public void testRunWithValueParameters() {
        try (Session session = driver.session()) {
            String personName = "Bob";
            int personAge = 40;
            session.run("CREATE (p:Person {name: '" + personName + "', age: " + personAge + "})");

            Value parameters = Values.parameters(PARAM_NAME, personName);

            ExecutionTracer.setExecutingInitNeo4J(false);
            Result result = (Result) Neo4JSessionClassReplacement.run_EM_0(session, MATCH_PERSON_BY_NAME_QUERY, parameters);

            assertTrue(result.hasNext());
            Record record = result.next();
            assertEquals(personAge, record.get(FIELD_AGE).asInt());

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<Neo4JRunCommand> neo4jCommands = additionalInfoList.get(0).getNeo4JInfoData();
            assertEquals(1, neo4jCommands.size());

            Neo4JRunCommand command = neo4jCommands.iterator().next();
            assertEquals(MATCH_PERSON_BY_NAME_QUERY, command.getQuery());
            Value capturedParams = (Value) command.getParameters();
            assertEquals(1, capturedParams.size());
            assertEquals(personName, capturedParams.get(PARAM_NAME).asString());
            assertTrue(command.getSuccessfullyExecuted());
            assertTrue(command.getExecutionTime() >= 0);
        }
    }

    @Test
    public void testRunWithRecordParameters() {
        try (Session session = driver.session()) {
            String personName = "Alice";
            int personAge = 35;
            session.run("CREATE (p:Person {name: '" + personName + "', age: " + personAge + "})");

            Result paramResult = session.run("RETURN '" + personName + "' AS " + PARAM_NAME);
            Record parameters = paramResult.next();

            ExecutionTracer.setExecutingInitNeo4J(false);
            Result result = (Result) Neo4JSessionClassReplacement.run_EM_1(session, MATCH_PERSON_BY_NAME_QUERY, parameters);

            assertTrue(result.hasNext());
            Record record = result.next();
            assertEquals(personAge, record.get(FIELD_AGE).asInt());

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<Neo4JRunCommand> neo4jCommands = additionalInfoList.get(0).getNeo4JInfoData();
            assertEquals(1, neo4jCommands.size());

            Neo4JRunCommand command = neo4jCommands.iterator().next();
            assertEquals(MATCH_PERSON_BY_NAME_QUERY, command.getQuery());
            Record capturedParams = (Record) command.getParameters();
            assertEquals(1, capturedParams.size());
            assertEquals(personName, capturedParams.get(PARAM_NAME).asString());
            assertTrue(command.getSuccessfullyExecuted());
            assertTrue(command.getExecutionTime() >= 0);
        }
    }

    @Test
    public void testMultipleQueriesTracked() {
        try (Session session = driver.session()) {
            String query1 = "RETURN 1 AS a";
            String query2 = "RETURN 2 AS b";
            String query3 = "RETURN 3 AS c";

            ExecutionTracer.setExecutingInitNeo4J(false);

            Neo4JSessionClassReplacement.run(session, query1);
            Neo4JSessionClassReplacement.run(session, query2);
            Neo4JSessionClassReplacement.run(session, query3);

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<Neo4JRunCommand> neo4jCommands = additionalInfoList.get(0).getNeo4JInfoData();
            assertEquals(3, neo4jCommands.size());

            Set<String> trackedQueries = neo4jCommands.stream()
                    .map(Neo4JRunCommand::getQuery)
                    .collect(Collectors.toSet());
            assertTrue(trackedQueries.contains(query1));
            assertTrue(trackedQueries.contains(query2));
            assertTrue(trackedQueries.contains(query3));

            for (Neo4JRunCommand command : neo4jCommands) {
                assertTrue(command.getSuccessfullyExecuted());
                assertTrue(command.getExecutionTime() >= 0);
            }
        }
    }

    @Test
    public void testInitNeo4JNotTracked() {
        try (Session session = driver.session()) {
            ExecutionTracer.setExecutingInitNeo4J(true);
            Neo4JSessionClassReplacement.run(session, "RETURN 1 AS number");

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<Neo4JRunCommand> neo4jCommands = additionalInfoList.get(0).getNeo4JInfoData();
            assertEquals(0, neo4jCommands.size());
        }
    }
}
