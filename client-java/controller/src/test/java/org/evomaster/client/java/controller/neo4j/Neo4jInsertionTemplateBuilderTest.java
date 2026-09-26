package org.evomaster.client.java.controller.neo4j;

import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jFailedQuery;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionKeyBuilder;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;
import org.evomaster.client.java.controller.neo4j.parser.CypherParser;
import org.evomaster.client.java.controller.neo4j.parser.CypherParserException;
import org.evomaster.client.java.controller.neo4j.parser.CypherParserFactory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4jInsertionTemplateBuilderTest {

    private static final CypherParser PARSER = CypherParserFactory.buildParser();

    private static Neo4jFailedQuery build(String cypher) {
        return build(cypher, Collections.emptyMap());
    }

    private static Neo4jFailedQuery build(String cypher, Map<String, Object> parameters) {
        try {
            return Neo4jInsertionTemplateBuilder.build(PARSER.parse(cypher), parameters, cypher);
        } catch (CypherParserException e) {
            throw new AssertionError("Test query does not parse: " + cypher, e);
        }
    }

    private static Map<String, Object> params(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(key, value);
        return m;
    }

    private static void assertProperty(Neo4jInsertionEntryDto property, String key, Neo4jPropertyTypeDto type, String value) {
        assertEquals(key, property.propertyKey);
        assertEquals(type, property.type);
        assertEquals(value, property.value);
    }

    @Test
    public void testNodeWithLabelsAndPropertyFromLiteral() {
        Neo4jFailedQuery failed = build("MATCH (p:Person:Employee {name: 'Ana'}) RETURN p");

        assertEquals(1, failed.nodes.size());
        assertTrue(failed.edges.isEmpty());
        Neo4jNodeInsertionDto person = failed.nodes.get(0);
        assertEquals(0L, person.id);
        assertEquals(Arrays.asList("Person", "Employee"), person.labels);
        assertEquals(1, person.properties.size());
        assertProperty(person.properties.get(0), "name", Neo4jPropertyTypeDto.STRING, "Ana");
        assertEquals("MATCH (p:Person:Employee {name: 'Ana'}) RETURN p", failed.query);
    }

    @Test
    public void testRelationshipBetweenPatternNodes() {
        String cypher = "MATCH (p:Player)-[:HAS_USER]->(u:User {username: $username}) RETURN p";

        Neo4jFailedQuery failed = build(cypher, params("username", "ana"));

        assertEquals(2, failed.nodes.size());
        assertEquals(Arrays.asList("Player"), failed.nodes.get(0).labels);
        assertEquals(Arrays.asList("User"), failed.nodes.get(1).labels);
        assertProperty(failed.nodes.get(1).properties.get(0), "username", Neo4jPropertyTypeDto.STRING, "ana");

        assertEquals(1, failed.edges.size());
        Neo4jEdgeInsertionDto edge = failed.edges.get(0);
        assertEquals("HAS_USER", edge.type);
        assertEquals(0L, edge.fromNodeId);
        assertEquals(1L, edge.toNodeId);
        assertTrue(edge.properties.isEmpty());
    }

    @Test
    public void testComparisonsInWhereSeedThePropertyWithTheValueComparedAgainst() {
        Neo4jFailedQuery failed = build(
                "MATCH (n:Person) WHERE n.age > 30 AND n.name STARTS WITH 'A' AND 1.5 <= n.score AND n.active = true RETURN n");

        Neo4jNodeInsertionDto person = failed.nodes.get(0);
        assertEquals(4, person.properties.size());
        assertProperty(person.properties.get(0), "age", Neo4jPropertyTypeDto.INTEGER, "30");
        assertProperty(person.properties.get(1), "name", Neo4jPropertyTypeDto.STRING, "A");
        assertProperty(person.properties.get(2), "score", Neo4jPropertyTypeDto.FLOAT, "1.5");
        assertProperty(person.properties.get(3), "active", Neo4jPropertyTypeDto.BOOLEAN, "true");
    }

    @Test
    public void testConditionsThatPinNoValueContributeNothing() {
        Neo4jFailedQuery failed = build(
                "MATCH (a:A)-[r:R]->(b:B) WHERE (a.x = 1 OR a.x = 2) AND NOT b.y = 3 AND a.z <> 4"
                        + " AND a.w IS NOT NULL AND a.v IN [1, 2] AND a.u = b.u RETURN a");

        assertTrue(failed.nodes.get(0).properties.isEmpty());
        assertTrue(failed.nodes.get(1).properties.isEmpty());
        assertEquals("R", failed.edges.get(0).type);
    }

    @Test
    public void testUnresolvedParameterGivesNoProperty() {
        Neo4jFailedQuery failed = build("MATCH (n:Person {name: $name}) RETURN n");

        assertTrue(failed.nodes.get(0).properties.isEmpty());

        Map<String, Object> boundToNull = new LinkedHashMap<>();
        boundToNull.put("name", null);
        assertTrue(build("MATCH (n:Person {name: $name}) RETURN n", boundToNull).nodes.get(0).properties.isEmpty());
    }

    @Test
    public void testFirstValueOfAPropertyWins() {
        Neo4jFailedQuery failed = build("MATCH (n:Person {age: 20}) WHERE n.age > 30 RETURN n");

        assertEquals(1, failed.nodes.get(0).properties.size());
        assertProperty(failed.nodes.get(0).properties.get(0), "age", Neo4jPropertyTypeDto.INTEGER, "20");
    }

    @Test
    public void testRelationshipPropertiesAndUndirectedEdge() {
        Neo4jFailedQuery failed = build("MATCH (a:A)-[r:R {since: 2020}]-(b:B) WHERE r.weight = 0.5 RETURN a");

        Neo4jEdgeInsertionDto edge = failed.edges.get(0);
        assertEquals("R", edge.type);
        assertEquals(0L, edge.fromNodeId);
        assertEquals(1L, edge.toNodeId);
        assertEquals(2, edge.properties.size());
        assertProperty(edge.properties.get(0), "since", Neo4jPropertyTypeDto.INTEGER, "2020");
        assertProperty(edge.properties.get(1), "weight", Neo4jPropertyTypeDto.FLOAT, "0.5");
    }

    @Test
    public void testVariableLengthEdgeIsUnrolled() {
        Neo4jFailedQuery failed = build("MATCH (a:A)-[:R*2]->(b:B) RETURN a");

        // the expander unrolls *2 into two hops through a fresh node, which inherits the labels of both ends
        assertEquals(3, failed.nodes.size());
        assertEquals(Arrays.asList("A"), failed.nodes.get(0).labels);
        assertEquals(Arrays.asList("B"), failed.nodes.get(1).labels);
        assertEquals(Arrays.asList("A", "B"), failed.nodes.get(2).labels);
        assertEquals(2, failed.edges.size());
        for (Neo4jEdgeInsertionDto edge : failed.edges) {
            assertEquals("R", edge.type);
        }
        assertEquals(0L, failed.edges.get(0).fromNodeId);
        assertEquals(2L, failed.edges.get(0).toNodeId);
        assertEquals(2L, failed.edges.get(1).fromNodeId);
        assertEquals(1L, failed.edges.get(1).toNodeId);
    }

    @Test
    public void testAnonymousNodesAreCreatedToo() {
        Neo4jFailedQuery failed = build("MATCH (:A)-[:R]->() RETURN 1");

        assertEquals(2, failed.nodes.size());
        assertEquals(Arrays.asList("A"), failed.nodes.get(0).labels);
        assertTrue(failed.nodes.get(1).labels.isEmpty());
        assertEquals(1, failed.edges.size());
    }

    @Test
    public void testEdgeWithoutTypeCannotBeCreated() {
        assertNull(build("MATCH (a:A)-[r]->(b:B) RETURN a"));
    }

    @Test
    public void testKeyTellsInsertionsApartByValueAndStructure() {
        String query = "MATCH (n:Person {name: $name}) RETURN n";
        Neo4jFailedQuery ana = build(query, params("name", "Ana"));
        Neo4jFailedQuery anaAgain = build(query, params("name", "Ana"));
        Neo4jFailedQuery luis = build(query, params("name", "Luis"));
        Neo4jFailedQuery related = build("MATCH (n:Person {name: 'Ana'})-[:KNOWS]->(:Person) RETURN n");

        String anaKey = Neo4jInsertionKeyBuilder.fromCommands(ana.nodes, ana.edges);
        assertEquals(anaKey, Neo4jInsertionKeyBuilder.fromCommands(anaAgain.nodes, anaAgain.edges));
        assertNotEquals(anaKey, Neo4jInsertionKeyBuilder.fromCommands(luis.nodes, luis.edges));
        assertNotEquals(anaKey, Neo4jInsertionKeyBuilder.fromCommands(related.nodes, related.edges));
    }
}
