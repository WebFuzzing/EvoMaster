package org.evomaster.client.java.instrumentation;

import com.foo.somedifferentpackage.examples.neo4j.AdopterNode;
import com.foo.somedifferentpackage.examples.neo4j.NotAnEntity;
import com.foo.somedifferentpackage.examples.neo4j.PlayerNode;
import com.foo.somedifferentpackage.examples.neo4j.UserNode;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class Neo4jEntityAnalyzerTest {

    @BeforeEach
    void reset() {
        UnitsInfoRecorder.reset();
    }

    @Test
    void testPropertiesOfSupportedTypesWithNamesAndIds() throws Exception {
        Neo4jEntity entity = Neo4jEntityAnalyzer.analyze(AdopterNode.class);

        assertEquals(AdopterNode.class.getName(), entity.getClassName());
        assertEquals(Arrays.asList("Adopter"), entity.getLabels());
        assertTrue(entity.getRelationships().isEmpty());

        assertEquals(Arrays.asList("id", "name", "budget", "hasYard", "max_dogs", "score", "level"),
                names(entity.getProperties()));
        assertProperty(entity, "id", "STRING", true, false);
        assertProperty(entity, "name", "STRING", false, false);
        assertProperty(entity, "budget", "INTEGER", false, false);
        assertProperty(entity, "hasYard", "BOOLEAN", false, false);
        assertProperty(entity, "max_dogs", "INTEGER", false, false);
        assertProperty(entity, "score", "FLOAT", false, false);
        assertProperty(entity, "level", "STRING", false, false);

        Neo4jEntityProperty budget = property(entity, "budget");
        assertEquals((long) Integer.MIN_VALUE, budget.getMinValue());
        assertEquals((long) Integer.MAX_VALUE, budget.getMaxValue());
        assertNull(property(entity, "max_dogs").getMinValue());
        assertTrue(property(entity, "name").getEnumValues().isEmpty());
        assertEquals(Arrays.asList("BRONZE", "GOLD"), property(entity, "level").getEnumValues());
    }

    @Test
    void testLabelsGeneratedIdAndInheritedProperties() throws Exception {
        Neo4jEntity entity = Neo4jEntityAnalyzer.analyze(UserNode.class);

        assertEquals(Arrays.asList("User", "Account"), entity.getLabels());
        assertEquals(Arrays.asList("createdBy", "id", "username"), names(entity.getProperties()));
        assertProperty(entity, "id", "INTEGER", true, true);
    }

    @Test
    void testRelationships() throws Exception {
        Neo4jEntity entity = Neo4jEntityAnalyzer.analyze(PlayerNode.class);

        assertEquals(Arrays.asList("PlayerNode"), entity.getLabels());
        assertEquals(Arrays.asList("id", "rating"), names(entity.getProperties()));

        List<Neo4jEntityRelationship> relationships = entity.getRelationships();
        assertEquals(3, relationships.size());

        assertEquals("HAS_USER", relationships.get(0).getType());
        assertEquals(UserNode.class.getName(), relationships.get(0).getTargetClassName());
        assertTrue(relationships.get(0).isOutgoing());

        assertEquals("PLAYED", relationships.get(1).getType());
        assertEquals(AdopterNode.class.getName(), relationships.get(1).getTargetClassName());
        assertFalse(relationships.get(1).isOutgoing());

        assertEquals("KNOWS", relationships.get(2).getType());
        assertEquals(PlayerNode.class.getName(), relationships.get(2).getTargetClassName());
    }

    @Test
    void testNotAnEntity() throws Exception {
        assertNull(Neo4jEntityAnalyzer.analyze(NotAnEntity.class));
    }

    @Test
    void testRecorderCollectsTheEntitiesOfTheCoveredClassesAlsoAfterAReset() {
        for (Class<?> k : Arrays.asList(AdopterNode.class, NotAnEntity.class, UserNode.class)) {
            UnitsInfoRecorder.registerClassLoader(k.getName(), k.getClassLoader());
            UnitsInfoRecorder.markNewUnit(k.getName());
        }

        List<String> expected = Arrays.asList(AdopterNode.class.getName(), UserNode.class.getName());
        assertEquals(expected, classNames(UnitsInfoRecorder.getInstance().getNeo4jEntities()));

        UnitsInfoRecorder.reset();
        assertEquals(0, UnitsInfoRecorder.getInstance().getNumberOfUnits());
        assertEquals(expected, classNames(UnitsInfoRecorder.getInstance().getNeo4jEntities()));
    }

    private static List<String> classNames(List<Neo4jEntity> entities) {
        return entities.stream().map(Neo4jEntity::getClassName).collect(Collectors.toList());
    }

    private static List<String> names(List<Neo4jEntityProperty> properties) {
        return properties.stream().map(Neo4jEntityProperty::getName).collect(Collectors.toList());
    }

    private static Neo4jEntityProperty property(Neo4jEntity entity, String name) {
        return entity.getProperties().stream()
                .filter(it -> it.getName().equals(name))
                .findFirst().orElseThrow(() -> new AssertionError("No property " + name));
    }

    private static void assertProperty(Neo4jEntity entity, String name, String type, boolean id, boolean generated) {
        Neo4jEntityProperty p = property(entity, name);
        assertEquals(type, p.getType(), name);
        assertEquals(id, p.isId(), name);
        assertEquals(generated, p.isGenerated(), name);
    }
}
