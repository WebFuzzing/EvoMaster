package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the validation, ordering, and immutability of primary-key metadata. */
public class DynamoDbPrimaryKeyTest {

    @Test
    public void testPreservesWorldCupKeyDefinition() {
        Map<String, DynamoDbScalarTypeDto> attributes = new LinkedHashMap<>();
        attributes.put("country", DynamoDbScalarTypeDto.STRING);
        attributes.put("fifaId", DynamoDbScalarTypeDto.NUMBER);

        DynamoDbPrimaryKey key = new DynamoDbPrimaryKey(attributes);

        assertEquals(Arrays.asList("country", "fifaId"), new ArrayList<>(key.getAttributes().keySet()));
        assertEquals(attributes, key.getAttributes());
    }

    @Test
    public void testCopiesSourceAttributes() {
        Map<String, DynamoDbScalarTypeDto> attributes = new LinkedHashMap<>();
        attributes.put("country", DynamoDbScalarTypeDto.STRING);
        DynamoDbPrimaryKey key = new DynamoDbPrimaryKey(attributes);

        attributes.clear();

        assertEquals(Collections.singletonMap("country", DynamoDbScalarTypeDto.STRING), key.getAttributes());
    }

    @Test
    public void testExposesImmutableAttributes() {
        DynamoDbPrimaryKey key = new DynamoDbPrimaryKey(
                Collections.singletonMap("country", DynamoDbScalarTypeDto.STRING));

        assertThrows(UnsupportedOperationException.class,
                () -> key.getAttributes().put("country", DynamoDbScalarTypeDto.NUMBER));
        assertThrows(UnsupportedOperationException.class,
                () -> key.getAttributes().keySet().remove("country"));
    }

    @Test
    public void testRejectsMissingAttributes() {
        assertThrows(NullPointerException.class, () -> new DynamoDbPrimaryKey(null));
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbPrimaryKey(Collections.emptyMap()));
    }

    @Test
    public void testRejectsMissingAttributeNames() {
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbPrimaryKey(
                Collections.singletonMap(null, DynamoDbScalarTypeDto.STRING)));
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbPrimaryKey(
                Collections.singletonMap("", DynamoDbScalarTypeDto.STRING)));
    }

    @Test
    public void testRejectsUnsupportedAttributeTypes() {
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbPrimaryKey(
                Collections.singletonMap("captain", DynamoDbScalarTypeDto.BOOLEAN)));
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbPrimaryKey(
                Collections.singletonMap("fifaId", null)));
    }
}
