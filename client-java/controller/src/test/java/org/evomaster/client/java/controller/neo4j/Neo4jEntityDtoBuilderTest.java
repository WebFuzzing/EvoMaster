package org.evomaster.client.java.controller.neo4j;

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityPropertyDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityRelationshipDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;
import org.evomaster.client.java.instrumentation.Neo4jEntity;
import org.evomaster.client.java.instrumentation.Neo4jEntityProperty;
import org.evomaster.client.java.instrumentation.Neo4jEntityRelationship;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4jEntityDtoBuilderTest {

    private static final String PERSON = "com.foo.Person";
    private static final String CITY = "com.foo.City";

    @Test
    public void testNoEntities() {
        assertTrue(Neo4jEntityDtoBuilder.build(Collections.emptyList()).isEmpty());
    }

    @Test
    public void testPropertiesAreCopied() {
        Neo4jEntityProperty id = new Neo4jEntityProperty("id", "INTEGER", true, true);
        Neo4jEntityProperty age = new Neo4jEntityProperty("age", "INTEGER", false, false,
                (long) Short.MIN_VALUE, (long) Short.MAX_VALUE, null);
        Neo4jEntityProperty role = new Neo4jEntityProperty("role", "STRING", false, false,
                null, null, Arrays.asList("ADMIN", "USER"));
        Neo4jEntity person = new Neo4jEntity(PERSON, Arrays.asList("Person", "Human"),
                Arrays.asList(id, age, role), Collections.emptyList());

        List<Neo4jEntityDto> dtos = Neo4jEntityDtoBuilder.build(Collections.singletonList(person));

        assertEquals(1, dtos.size());
        Neo4jEntityDto dto = dtos.get(0);
        assertEquals(PERSON, dto.className);
        assertEquals(Arrays.asList("Person", "Human"), dto.labels);
        assertTrue(dto.relationships.isEmpty());
        assertEquals(3, dto.properties.size());

        Neo4jEntityPropertyDto idDto = dto.properties.get(0);
        assertEquals("id", idDto.name);
        assertEquals(Neo4jPropertyTypeDto.INTEGER, idDto.type);
        assertTrue(idDto.isId);
        assertTrue(idDto.isGenerated);
        assertNull(idDto.minValue);
        assertNull(idDto.maxValue);
        assertTrue(idDto.enumValues.isEmpty());

        Neo4jEntityPropertyDto ageDto = dto.properties.get(1);
        assertFalse(ageDto.isId);
        assertFalse(ageDto.isGenerated);
        assertEquals((long) Short.MIN_VALUE, ageDto.minValue);
        assertEquals((long) Short.MAX_VALUE, ageDto.maxValue);

        Neo4jEntityPropertyDto roleDto = dto.properties.get(2);
        assertEquals(Neo4jPropertyTypeDto.STRING, roleDto.type);
        assertEquals(Arrays.asList("ADMIN", "USER"), roleDto.enumValues);
    }

    @Test
    public void testRelationshipTargetsBecomeLabels() {
        Neo4jEntity person = new Neo4jEntity(PERSON, Collections.singletonList("Person"),
                Collections.emptyList(),
                Arrays.asList(new Neo4jEntityRelationship("LIVES_IN", CITY, true),
                        new Neo4jEntityRelationship("VISITED_BY", CITY, false)));
        Neo4jEntity city = new Neo4jEntity(CITY, Arrays.asList("City", "Place"),
                Collections.emptyList(), Collections.emptyList());

        List<Neo4jEntityDto> dtos = Neo4jEntityDtoBuilder.build(Arrays.asList(person, city));

        assertEquals(2, dtos.size());
        List<Neo4jEntityRelationshipDto> rels = dtos.get(0).relationships;
        assertEquals(2, rels.size());
        assertEquals("LIVES_IN", rels.get(0).type);
        assertEquals(Arrays.asList("City", "Place"), rels.get(0).targetLabels);
        assertTrue(rels.get(0).isOutgoing);
        assertEquals("VISITED_BY", rels.get(1).type);
        assertEquals(Arrays.asList("City", "Place"), rels.get(1).targetLabels);
        assertFalse(rels.get(1).isOutgoing);
        assertTrue(dtos.get(1).relationships.isEmpty());
    }

    @Test
    public void testRelationshipToUnknownClassIsDropped() {
        Neo4jEntity person = new Neo4jEntity(PERSON, Collections.singletonList("Person"),
                Collections.emptyList(),
                Collections.singletonList(new Neo4jEntityRelationship("OWNS", "com.foo.NotAnEntity", true)));

        List<Neo4jEntityDto> dtos = Neo4jEntityDtoBuilder.build(Collections.singletonList(person));

        assertEquals(1, dtos.size());
        assertTrue(dtos.get(0).relationships.isEmpty());
    }
}
