package org.evomaster.client.java.controller.neo4j.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jDatabaseCommandsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.evomaster.client.java.controller.neo4j.dsl.Neo4jDsl.neo4j;
import static org.junit.jupiter.api.Assertions.*;

public class Neo4jDslTest {

    @Test
    public void testBuildsNodesAndEdges() {
        Neo4jDatabaseCommandsDto commands = neo4j()
                .createNode(1L, "Player")
                .createNode(2L, "User", "Admin").d("username", "'ana'")
                .createEdge("HAS_USER", 1L, 2L).d("since", "2020")
                .dtos();

        assertEquals(2, commands.nodes.size());
        assertEquals(1, commands.edges.size());

        Neo4jNodeInsertionDto player = commands.nodes.get(0);
        assertEquals(1L, player.id);
        assertEquals(Arrays.asList("Player"), player.labels);
        assertTrue(player.properties.isEmpty());

        Neo4jNodeInsertionDto user = commands.nodes.get(1);
        assertEquals(2L, user.id);
        assertEquals(Arrays.asList("User", "Admin"), user.labels);
        assertEntry(user.properties.get(0), "username", Neo4jPropertyTypeDto.STRING, "ana");

        Neo4jEdgeInsertionDto edge = commands.edges.get(0);
        assertEquals("HAS_USER", edge.type);
        assertEquals(1L, edge.fromNodeId);
        assertEquals(2L, edge.toNodeId);
        assertEntry(edge.properties.get(0), "since", Neo4jPropertyTypeDto.INTEGER, "2020");
    }

    @Test
    public void testInfersThePropertyType() {
        Neo4jNodeInsertionDto node = neo4j()
                .createNode(1L)
                .d("string", "'it''s'")
                .d("numeric string", "'30'")
                .d("integer", "-30")
                .d("float", "1.5")
                .d("exponent", "2e3")
                .d("beyond 64 bits", "99999999999999999999")
                .d("boolean", "TRUE")
                .dtos().nodes.get(0);

        assertEntry(node.properties.get(0), "string", Neo4jPropertyTypeDto.STRING, "it's");
        assertEntry(node.properties.get(1), "numeric string", Neo4jPropertyTypeDto.STRING, "30");
        assertEntry(node.properties.get(2), "integer", Neo4jPropertyTypeDto.INTEGER, "-30");
        assertEntry(node.properties.get(3), "float", Neo4jPropertyTypeDto.FLOAT, "1.5");
        assertEntry(node.properties.get(4), "exponent", Neo4jPropertyTypeDto.FLOAT, "2e3");
        assertEntry(node.properties.get(5), "beyond 64 bits", Neo4jPropertyTypeDto.FLOAT, "99999999999999999999");
        assertEntry(node.properties.get(6), "boolean", Neo4jPropertyTypeDto.BOOLEAN, "true");
    }

    @Test
    public void testRejectsAValueOfNoSupportedType() {
        assertThrows(IllegalArgumentException.class, () -> neo4j().createNode(1L).d("p", "unquoted"));
        assertThrows(IllegalArgumentException.class, () -> neo4j().createNode(1L).d("p", "NaN"));
        assertThrows(IllegalArgumentException.class, () -> neo4j().createNode(1L).d("p", "1d"));
        assertThrows(IllegalArgumentException.class, () -> neo4j().createNode(1L).d("p", null));
    }

    @Test
    public void testRejectsIncompleteStatements() {
        assertThrows(IllegalArgumentException.class, () -> neo4j().createNode(null, "Person"));
        assertThrows(IllegalArgumentException.class, () -> neo4j().createEdge("", 1L, 2L));
        assertThrows(IllegalArgumentException.class, () -> neo4j().createEdge("KNOWS", null, 2L));
        assertThrows(IllegalArgumentException.class, () -> neo4j().createNode(1L).d("", "1"));
    }

    @Test
    public void testCannotBeUsedOnceBuilt() {
        Neo4jStatementDsl dsl = neo4j().createNode(1L, "Person");
        dsl.dtos();

        assertThrows(IllegalStateException.class, dsl::dtos);
        assertThrows(IllegalStateException.class, () -> dsl.createNode(2L, "Person"));
        assertThrows(IllegalStateException.class, () -> dsl.d("p", "1"));
    }

    private static void assertEntry(Neo4jInsertionEntryDto entry, String key, Neo4jPropertyTypeDto type, String value) {
        assertEquals(key, entry.propertyKey);
        assertEquals(type, entry.type);
        assertEquals(value, entry.value);
    }
}
