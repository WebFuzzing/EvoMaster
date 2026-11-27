package org.evomaster.client.java.controller.api.dto.database.operations;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests {@link DynamoDbAttributeValueDto} constructor validation and value/key type mappings. */
public class DynamoDbAttributeValueDtoTest {

    @Test
    public void rejectsNonScalarTypeInScalarConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbAttributeValueDto("squad", DynamoDbValueTypeDto.MAP, "{}"));
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbAttributeValueDto("players", DynamoDbValueTypeDto.LIST, "[]"));
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbAttributeValueDto("tags", DynamoDbValueTypeDto.STRING_SET, "a"));
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbAttributeValueDto("scores", DynamoDbValueTypeDto.NUMBER_SET, "1"));
        assertThrows(IllegalArgumentException.class, () -> new DynamoDbAttributeValueDto("coach", DynamoDbValueTypeDto.NULL, null));
    }

    @Test
    public void mapsEveryKeyScalarTypeToValueType() {
        for (DynamoDbKeyScalarTypeDto keyType : DynamoDbKeyScalarTypeDto.values()) {
            DynamoDbValueTypeDto mapped = DynamoDbValueTypeDto.fromKeyScalar(keyType);
            assertNotNull(mapped);
            assertEquals(keyType.name(), mapped.name());

            DynamoDbAttributeValueDto dto = new DynamoDbAttributeValueDto("key", keyType, "val");
            assertEquals(mapped, dto.type);
            assertEquals("val", dto.value);
        }

        assertNull(DynamoDbValueTypeDto.fromKeyScalar(null));
        DynamoDbAttributeValueDto nullTypedDto = new DynamoDbAttributeValueDto("key", (DynamoDbKeyScalarTypeDto) null, "val");
        assertNull(nullTypedDto.type);
    }

    @Test
    public void mapsSupportedWireCodesAndRejectsBinaryOrUnknownCodes() {
        for (DynamoDbValueTypeDto valueType : DynamoDbValueTypeDto.values()) {
            assertEquals(valueType, DynamoDbValueTypeDto.fromWireCode(valueType.wireCode));
        }

        assertThrows(IllegalArgumentException.class,
                () -> DynamoDbValueTypeDto.fromWireCode(DynamoDbValueTypeDto.BINARY_WIRE_CODE));
        assertThrows(IllegalArgumentException.class,
                () -> DynamoDbValueTypeDto.fromWireCode(DynamoDbValueTypeDto.BINARY_SET_WIRE_CODE));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbValueTypeDto.fromWireCode("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbValueTypeDto.fromWireCode(null));
    }
}
