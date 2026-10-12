package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.Neo4JRunCommand;
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

class Neo4JOperationClassReplacementTest {

    private static final String QUERY = "MATCH (p:Person {name: $name}) RETURN p";

    @Test
    void testNoParametersIsAnEmptyMap() {
        assertTrue(Neo4JOperationClassReplacement.toPlainParameters(null).isEmpty());
    }

    @Test
    void testDriverValuesInsideAParameterMapAreUnwrapped() {
        Map<String, Object> taken = new LinkedHashMap<>();
        taken.put("name", "Ana");
        taken.put("age", Values.value(25L));
        taken.put("tags", Values.value(Arrays.asList("a", "b")));

        Map<String, Object> parameters = Neo4JOperationClassReplacement.toPlainParameters(taken);

        assertEquals("Ana", parameters.get("name"));
        assertEquals(25L, parameters.get("age"));
        assertEquals(Arrays.asList("a", "b"), parameters.get("tags"));
    }

    @Test
    void testParametersTakenAsADriverValueAreReadAsAMap() {
        Value taken = Values.parameters("name", "Ana", "address", Collections.singletonMap("city", "Lima"));

        Map<String, Object> parameters = Neo4JOperationClassReplacement.toPlainParameters(taken);

        assertEquals("Ana", parameters.get("name"));
        assertEquals(Collections.singletonMap("city", "Lima"), parameters.get("address"));
    }

    @Test
    void testAnUnknownParameterShapeYieldsNoParameters() {
        assertTrue(Neo4JOperationClassReplacement.toPlainParameters("name=Ana").isEmpty());
    }

    @Test
    void testAValueThatCannotBeSerializedIsKeptAsText() {
        Object notSerializable = new Object() {
            @Override
            public String toString() {
                return "opaque";
            }
        };

        Map<String, Object> parameters = Neo4JOperationClassReplacement.toPlainParameters(
                Collections.singletonMap("p", notSerializable));

        assertEquals("opaque", parameters.get("p"));
    }

    @Test
    void testACommandWithParametersTakenFromTheDriverIsSerialized() throws Exception {
        Map<String, Object> taken = new LinkedHashMap<>();
        taken.put("name", Values.value("Ana"));
        taken.put("point", Values.point(4326, 1.0, 2.0));
        Neo4JRunCommand command = new Neo4JRunCommand(
                QUERY, Neo4JOperationClassReplacement.toPlainParameters(taken), true, 7);

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
