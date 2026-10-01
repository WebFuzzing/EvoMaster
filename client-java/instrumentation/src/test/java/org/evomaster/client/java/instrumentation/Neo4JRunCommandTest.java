package org.evomaster.client.java.instrumentation;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Neo4JRunCommandTest {

    private static final String QUERY = "MATCH (p:Person {name: $name}) RETURN p";

    @Test
    void testNoParametersIsAnEmptyMap() {
        assertTrue(new Neo4JRunCommand(QUERY, null, true, 1).getParameters().isEmpty());
    }

    @Test
    void testDriverValuesInsideAParameterMapAreUnwrapped() {
        Map<String, Object> captured = new LinkedHashMap<>();
        captured.put("name", "Ana");
        captured.put("age", Values.value(25L));
        captured.put("tags", Values.value(Arrays.asList("a", "b")));

        Map<String, Object> parameters = new Neo4JRunCommand(QUERY, captured, true, 1).getParameters();

        assertEquals("Ana", parameters.get("name"));
        assertEquals(25L, parameters.get("age"));
        assertEquals(Arrays.asList("a", "b"), parameters.get("tags"));
    }

    @Test
    void testParametersTakenAsADriverValueAreReadAsAMap() {
        Value captured = Values.parameters("name", "Ana", "address", Collections.singletonMap("city", "Lima"));

        Map<String, Object> parameters = new Neo4JRunCommand(QUERY, captured, true, 1).getParameters();

        assertEquals("Ana", parameters.get("name"));
        assertEquals(Collections.singletonMap("city", "Lima"), parameters.get("address"));
    }

    @Test
    void testAnUnknownParameterShapeYieldsNoParameters() {
        assertTrue(new Neo4JRunCommand(QUERY, "name=Ana", true, 1).getParameters().isEmpty());
    }

    @Test
    void testAValueThatCannotBeSerializedIsKeptAsText() {
        Object notSerializable = new Object() {
            @Override
            public String toString() {
                return "opaque";
            }
        };

        Map<String, Object> parameters = new Neo4JRunCommand(
                QUERY, Collections.singletonMap("p", notSerializable), true, 1).getParameters();

        assertEquals("opaque", parameters.get("p"));
    }

    @Test
    void testIsSerializedWithParametersTakenFromTheDriver() throws Exception {
        Map<String, Object> captured = new LinkedHashMap<>();
        captured.put("name", Values.value("Ana"));
        captured.put("point", Values.point(4326, 1.0, 2.0));
        Neo4JRunCommand command = new Neo4JRunCommand(QUERY, captured, true, 7);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(command);
        }
        Neo4JRunCommand copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (Neo4JRunCommand) in.readObject();
        }

        assertEquals(QUERY, copy.getQuery());
        assertEquals("Ana", copy.getParameters().get("name"));
        assertTrue(copy.getParameters().containsKey("point"));
        assertEquals(7, copy.getExecutionTime());
    }
}
