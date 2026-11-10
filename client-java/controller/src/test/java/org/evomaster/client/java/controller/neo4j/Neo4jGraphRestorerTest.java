package org.evomaster.client.java.controller.neo4j;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4jGraphRestorerTest {

    private static final int NEO4J_BOLT_PORT = 7687;

    private static final GenericContainer<?> neo4jContainer = new GenericContainer<>("neo4j:5")
            .withExposedPorts(NEO4J_BOLT_PORT)
            .withEnv("NEO4J_AUTH", "none");

    private static Driver driver;
    private static ReflectionBasedNeo4jClient client;

    /** Two adopters, a dog adopted by the first one, and a shelter that is home to the dog. */
    private static final String SEED = "CREATE (a:Adopter:Person {id: 'P1', name: 'Ana', budget: 100}), "
            + "(b:Adopter {id: 'P2', name: 'Luis'}), (d:Dog {id: 'D1', size: 'SMALL'}), (s:Shelter {id: 'S1'}), "
            + "(a)-[:ADOPTED {year: 2020}]->(d), (s)-[:HOUSES]->(d)";

    /** What the restore has to preserve: the labels and properties of every node... */
    private static final String NODES_QUERY = "MATCH (n) RETURN labels(n) AS l, properties(n) AS p";
    /** ...and the type, properties and endpoints of every relationship. */
    private static final String RELATIONSHIPS_QUERY = "MATCH (a)-[r]->(b) RETURN a.id AS a, type(r) AS t, properties(r) AS p, b.id AS b";

    private Neo4jGraphRestorer restorer;

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

    @BeforeEach
    public void seed() {
        run(SEED);
        restorer = new Neo4jGraphRestorer();
        restorer.takeSnapshot(client);
    }

    @AfterEach
    public void cleanUp() {
        client.detachDeleteAll();
    }

    @Test
    public void testInsertedNodesAndRelationshipsAreDeletedEvenWhenTheyRepeatASeededId() {
        List<String> before = state();
        run("CREATE (:Adopter {id: 'P1', name: 'Ana', budget: 100})");
        run("MATCH (a:Adopter {id: 'P2'}), (d:Dog) CREATE (a)-[:ADOPTED]->(d), (a)-[:LIKES]->(:Toy {id: 'T1'})");

        restorer.restore(client);

        assertEquals(before, state());
    }

    @Test
    public void testChangedPropertiesAndLabelsAreRestored() {
        List<String> before = state();
        run("MATCH (a:Adopter {id: 'P1'}) SET a.budget = 5, a.extra = true REMOVE a:Person SET a:Banned");
        run("MATCH ()-[r:ADOPTED]->() SET r.year = 1999");
        run("MATCH (d:Dog) REMOVE d.size");

        restorer.restore(client);

        assertEquals(before, state());
    }

    @Test
    public void testDeletedNodesAndRelationshipsAreCreatedAgainAndCanBeRestoredTwice() {
        List<String> before = state();
        run("MATCH (d:Dog) DETACH DELETE d");
        run("MATCH (b:Adopter {id: 'P2'}) DETACH DELETE b");

        restorer.restore(client);
        assertEquals(before, state());

        // the recreated elements have new ids, which the next restore has to follow
        run("MATCH (d:Dog) SET d.size = 'LARGE'");
        run("MATCH ()-[r:HOUSES]->() DELETE r");

        restorer.restore(client);
        assertEquals(before, state());
    }

    @Test
    public void testAnUnchangedGraphIsLeftUntouched() {
        List<String> idsBefore = elementIds();

        restorer.restore(client);

        assertEquals(idsBefore, elementIds());
    }

    @Test
    public void testRestoringWithoutASnapshotFails() {
        assertThrows(IllegalStateException.class, () -> new Neo4jGraphRestorer().restore(client));

        assertTrue(restorer.hasSnapshot());
        restorer.discardSnapshot();
        assertFalse(restorer.hasSnapshot());
        assertThrows(IllegalStateException.class, () -> restorer.restore(client));
    }

    private static List<String> state() {
        List<String> lines = query(NODES_QUERY).stream()
                .map(r -> "node " + sorted(r.get("l").asList()) + " " + r.get("p").asMap())
                .collect(Collectors.toList());
        lines.addAll(query(RELATIONSHIPS_QUERY).stream()
                .map(r -> "rel " + r.get("a").asObject() + " " + r.get("t").asString() + " " + r.get("p").asMap()
                        + " " + r.get("b").asObject())
                .collect(Collectors.toList()));
        return lines.stream().map(Neo4jGraphRestorerTest::sortedMap).sorted().collect(Collectors.toList());
    }

    private static List<String> elementIds() {
        List<String> ids = query("MATCH (n) RETURN elementId(n) AS id").stream()
                .map(r -> r.get("id").asString()).collect(Collectors.toList());
        ids.addAll(query("MATCH ()-[r]->() RETURN elementId(r) AS id").stream()
                .map(r -> r.get("id").asString()).collect(Collectors.toList()));
        ids.sort(String::compareTo);
        return ids;
    }

    private static String sorted(List<Object> labels) {
        return labels.stream().map(String::valueOf).sorted().collect(Collectors.toList()).toString();
    }

    /** Property maps print in no fixed order, so the entries are sorted to compare lines. */
    private static String sortedMap(String line) {
        int open = line.indexOf('{');
        int close = line.indexOf('}');
        if (open < 0 || close < open) {
            return line;
        }
        String entries = Arrays.stream(line.substring(open + 1, close).split(", "))
                .sorted().collect(Collectors.joining(", "));
        return line.substring(0, open + 1) + entries + line.substring(close);
    }

    private static List<Record> query(String cypher) {
        try (Session session = driver.session()) {
            return session.run(cypher).list();
        }
    }

    private static void run(String cypher) {
        try (Session session = driver.session()) {
            session.run(cypher).consume();
        }
    }
}
