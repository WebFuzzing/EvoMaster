package org.evomaster.client.java.controller.neo4j;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jDatabaseCommandsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.List;

import static org.evomaster.client.java.controller.neo4j.dsl.Neo4jDsl.neo4j;
import static org.junit.jupiter.api.Assertions.*;

public class Neo4jScriptRunnerTest {

    private static final int NEO4J_BOLT_PORT = 7687;

    private static final GenericContainer<?> neo4jContainer = new GenericContainer<>("neo4j:5")
            .withExposedPorts(NEO4J_BOLT_PORT)
            .withEnv("NEO4J_AUTH", "none");

    private static Driver driver;
    private static ReflectionBasedNeo4jClient client;

    @BeforeAll
    public static void initClass() {
        neo4jContainer.start();
        String boltUrl = "bolt://" + neo4jContainer.getHost() + ":" + neo4jContainer.getMappedPort(NEO4J_BOLT_PORT);
        driver = GraphDatabase.driver(boltUrl, AuthTokens.none());
        driver.verifyConnectivity();
        client = new ReflectionBasedNeo4jClient(driver);
    }

    @AfterAll
    public static void closeClass() {
        if (driver != null) {
            driver.close();
        }
        neo4jContainer.stop();
    }

    @AfterEach
    public void cleanUp() {
        client.detachDeleteAll();
    }

    @Test
    public void testInsertNodeKeepsThePropertyTypes() {
        Neo4jDatabaseCommandsDto commands = neo4j()
                .createNode(1L, "Person")
                .d("name", "'Ana'")
                .d("age", "30")
                .d("score", "1.5")
                .d("active", "true")
                .dtos();

        Neo4jInsertionResultsDto results = Neo4jScriptRunner.executeInsert(client, commands);

        assertEquals(Arrays.asList(true), results.nodeExecutionResults);
        assertTrue(results.edgeExecutionResults.isEmpty());

        List<Record> records = query("MATCH (n:Person) RETURN elementId(n) AS id, n.name AS name, "
                + "n.age AS age, n.score AS score, n.active AS active");
        assertEquals(1, records.size());
        Record person = records.get(0);
        assertEquals("Ana", person.get("name").asObject());
        assertEquals(30L, person.get("age").asObject());
        assertEquals(1.5d, person.get("score").asObject());
        assertEquals(true, person.get("active").asObject());
        assertEquals(person.get("id").asString(), results.idMapping.get(1L));
    }

    @Test
    public void testInsertedIntegerSatisfiesANumericComparison() {
        Neo4jScriptRunner.executeInsert(client, neo4j().createNode(1L, "Person").d("age", "31").dtos());

        assertEquals(1, query("MATCH (n:Person) WHERE n.age > 30 RETURN n").size());
    }

    @Test
    public void testInsertNodeWithSeveralLabelsAndWithNone() {
        Neo4jDatabaseCommandsDto commands = neo4j()
                .createNode(1L, "Person", "Employee")
                .createNode(2L)
                .dtos();

        Neo4jInsertionResultsDto results = Neo4jScriptRunner.executeInsert(client, commands);

        assertEquals(Arrays.asList(true, true), results.nodeExecutionResults);
        assertEquals(1, query("MATCH (n:Person:Employee) RETURN n").size());
        assertEquals(1, query("MATCH (n) WHERE size(labels(n)) = 0 RETURN n").size());
    }

    @Test
    public void testInsertEdgeBetweenInsertedNodes() {
        Neo4jDatabaseCommandsDto commands = neo4j()
                .createNode(1L, "Player")
                .createNode(2L, "User").d("username", "'ana'")
                .createEdge("HAS_USER", 1L, 2L).d("since", "2020")
                .dtos();

        Neo4jInsertionResultsDto results = Neo4jScriptRunner.executeInsert(client, commands);

        assertEquals(Arrays.asList(true, true), results.nodeExecutionResults);
        assertEquals(Arrays.asList(true), results.edgeExecutionResults);

        List<Record> records = query(
                "MATCH (p:Player)-[r:HAS_USER]->(u:User {username: 'ana'}) RETURN r.since AS since");
        assertEquals(1, records.size());
        assertEquals(2020L, records.get(0).get("since").asObject());

        // the relationship is directed from the first node to the second
        assertEquals(0, query("MATCH (u:User)-[:HAS_USER]->(p:Player) RETURN u").size());
    }

    @Test
    public void testEdgeConnectsTheNodesOfThisInsertionOnly() {
        // an equal node that was already there must not end up connected
        query("CREATE (:Player)");

        Neo4jScriptRunner.executeInsert(client, neo4j()
                .createNode(1L, "Player")
                .createNode(2L, "User")
                .createEdge("HAS_USER", 1L, 2L)
                .dtos());

        assertEquals(2, query("MATCH (p:Player) RETURN p").size());
        assertEquals(1, query("MATCH (:Player)-[r:HAS_USER]->(:User) RETURN r").size());
    }

    @Test
    public void testFailedInsertionDoesNotStopTheNextOnes() {
        Neo4jNodeInsertionDto broken = node(1L, "Person");
        broken.properties.add(new Neo4jInsertionEntryDto("age", Neo4jPropertyTypeDto.INTEGER, "not a number"));

        Neo4jDatabaseCommandsDto commands = new Neo4jDatabaseCommandsDto();
        commands.nodes.add(broken);
        commands.nodes.add(node(2L, "Person"));
        commands.nodes.add(node(3L, "Person"));
        commands.edges.add(edge("KNOWS", 1L, 2L));
        commands.edges.add(edge("KNOWS", 2L, 3L));

        Neo4jInsertionResultsDto results = Neo4jScriptRunner.executeInsert(client, commands);

        assertEquals(Arrays.asList(false, true, true), results.nodeExecutionResults);
        assertEquals(Arrays.asList(false, true), results.edgeExecutionResults);
        assertFalse(results.idMapping.containsKey(1L));
        assertEquals(2, query("MATCH (n:Person) RETURN n").size());
        assertEquals(1, query("MATCH (:Person)-[r:KNOWS]->(:Person) RETURN r").size());
    }

    @Test
    public void testRepeatedNodeIdFails() {
        Neo4jDatabaseCommandsDto commands = new Neo4jDatabaseCommandsDto();
        commands.nodes.add(node(1L, "Person"));
        commands.nodes.add(node(1L, "Person"));

        Neo4jInsertionResultsDto results = Neo4jScriptRunner.executeInsert(client, commands);

        assertEquals(Arrays.asList(true, false), results.nodeExecutionResults);
        assertEquals(1, query("MATCH (n:Person) RETURN n").size());
    }

    @Test
    public void testLabelIsNeverReadAsCypher() {
        query("CREATE (:Witness)");
        String label = "Odd`) DETACH DELETE n //";

        Neo4jInsertionResultsDto results = Neo4jScriptRunner.executeInsert(client,
                neo4j().createNode(1L, label).dtos());

        assertEquals(Arrays.asList(true), results.nodeExecutionResults);
        assertEquals(1, query("MATCH (n:Witness) RETURN n").size());
        List<Record> records = query("MATCH (n) WHERE NOT n:Witness RETURN labels(n) AS labels");
        assertEquals(1, records.size());
        assertEquals(Arrays.asList(label), records.get(0).get("labels").asList());
    }

    @Test
    public void testNothingToInsertThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Neo4jScriptRunner.executeInsert(client, new Neo4jDatabaseCommandsDto()));
        assertThrows(IllegalArgumentException.class,
                () -> Neo4jScriptRunner.executeInsert(client, null));
    }

    private static Neo4jNodeInsertionDto node(Long id, String label) {
        Neo4jNodeInsertionDto node = new Neo4jNodeInsertionDto();
        node.id = id;
        node.labels.add(label);
        return node;
    }

    private static Neo4jEdgeInsertionDto edge(String type, Long fromNodeId, Long toNodeId) {
        Neo4jEdgeInsertionDto edge = new Neo4jEdgeInsertionDto();
        edge.type = type;
        edge.fromNodeId = fromNodeId;
        edge.toNodeId = toNodeId;
        return edge;
    }

    private static List<Record> query(String cypher) {
        try (Session session = driver.session()) {
            return session.run(cypher).list();
        }
    }
}
