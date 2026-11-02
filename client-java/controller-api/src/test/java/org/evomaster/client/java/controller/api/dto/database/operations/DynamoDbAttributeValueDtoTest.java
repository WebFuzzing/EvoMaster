package org.evomaster.client.java.controller.api.dto.database.operations;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests recursive DynamoDB values independently of their JSON transport encoding. */
public class DynamoDbAttributeValueDtoTest {

    /** Mixed nested payloads retain exact numbers, empty containers and nulls. */
    @Test
    public void preservesNativeWorldCupValues() {
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", "Lionel Messi");
        player.put("score", new BigDecimal("10.1234567890123456789"));
        player.put("captain", true);
        player.put("coach", null);
        player.put("matches", Collections.emptyList());
        player.put("profile", Collections.emptyMap());
        DynamoDbAttributeValueDto dto = DynamoDbAttributeValueDto.fromPlainValue("players", Arrays.asList(player, "Argentina", null));
        assertEquals(DynamoDbValueTypeDto.LIST, dto.type);
        assertNull(dto.value);
        assertEquals(DynamoDbValueTypeDto.MAP, dto.listValue.get(0).type);
        assertEquals(DynamoDbValueTypeDto.NULL, dto.listValue.get(2).type);
        assertEquals("10.1234567890123456789", dto.listValue.get(0).mapValue.get("score").value);
        assertEquals(Arrays.asList(player, "Argentina", null), dto.toPlainValue());
        DynamoDbAttributeValueDto copy = dto.copy();
        dto.listValue.get(0).mapValue.clear();
        assertEquals("Lionel Messi", copy.listValue.get(0).mapValue.get("name").value);
    }

    @Test
    public void rejectsSetsAndBinaryTemplates() {
        assertThrows(IllegalArgumentException.class, () -> DynamoDbAttributeValueDto.fromPlainValue("countries", Collections.singleton("Argentina")));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbAttributeValueDto.fromPlainValue("flag", Collections.singletonMap("image", new byte[]{1})));
    }

    @Test
    public void keepsScalarMetadataSeparate() {
        assertEquals(Arrays.asList(DynamoDbKeyScalarTypeDto.STRING, DynamoDbKeyScalarTypeDto.NUMBER, DynamoDbKeyScalarTypeDto.BOOLEAN),
                Arrays.asList(DynamoDbKeyScalarTypeDto.values()));
        assertEquals("S", DynamoDbKeyScalarTypeDto.STRING.wireCode);
        assertEquals("N", DynamoDbKeyScalarTypeDto.NUMBER.wireCode);
        assertEquals("BOOL", DynamoDbKeyScalarTypeDto.BOOLEAN.wireCode);
        assertEquals("S", DynamoDbValueTypeDto.STRING.wireCode);
        assertEquals("N", DynamoDbValueTypeDto.NUMBER.wireCode);
        assertEquals("BOOL", DynamoDbValueTypeDto.BOOLEAN.wireCode);
        assertEquals("M", DynamoDbValueTypeDto.MAP.wireCode);
        assertEquals("L", DynamoDbValueTypeDto.LIST.wireCode);
        assertEquals("NULL", DynamoDbValueTypeDto.NULL.wireCode);
    }
}
