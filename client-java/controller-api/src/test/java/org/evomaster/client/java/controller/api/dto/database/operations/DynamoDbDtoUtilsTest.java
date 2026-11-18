package org.evomaster.client.java.controller.api.dto.database.operations;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests recursive DynamoDB DTO creation and conversion via {@link DynamoDbDtoUtils}. */
public class DynamoDbDtoUtilsTest {

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

        DynamoDbAttributeValueDto dto = DynamoDbDtoUtils.fromPlainValue("players", Arrays.asList(player, "Argentina", null));
        assertEquals(DynamoDbValueTypeDto.LIST, dto.type);
        assertNull(dto.value);
        assertEquals(DynamoDbValueTypeDto.MAP, dto.listValue.get(0).type);
        assertEquals(DynamoDbValueTypeDto.NULL, dto.listValue.get(2).type);
        assertEquals("10.1234567890123456789", dto.listValue.get(0).mapValue.get("score").value);
        assertEquals(Arrays.asList(player, "Argentina", null), DynamoDbDtoUtils.toPlainValue(dto));
    }

    @Test
    public void supportsStringAndNumberSetsWithDeterministicOrdering() {
        Set<String> countries = new LinkedHashSet<>(Arrays.asList("Uruguay", "Argentina", "Brazil"));
        DynamoDbAttributeValueDto ssDto = DynamoDbDtoUtils.fromPlainValue("countries", countries);
        assertEquals(DynamoDbValueTypeDto.STRING_SET, ssDto.type);
        assertEquals(Arrays.asList("Argentina", "Brazil", "Uruguay"), ssDto.setValue);
        assertEquals(countries, DynamoDbDtoUtils.toPlainValue(ssDto));

        Set<Number> numbers = new LinkedHashSet<>(Arrays.asList(
                new BigDecimal("10.50"), 2, 10L, new BigDecimal("10.50")));
        DynamoDbAttributeValueDto nsDto = DynamoDbDtoUtils.fromPlainValue("scores", numbers);
        assertEquals(DynamoDbValueTypeDto.NUMBER_SET, nsDto.type);
        assertEquals(Arrays.asList("2", "10", "10.50"), nsDto.setValue);
        Set<BigDecimal> expectedNumbers = new LinkedHashSet<>(Arrays.asList(
                new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("10.50")));
        assertEquals(expectedNumbers, DynamoDbDtoUtils.toPlainValue(nsDto));
    }

    @Test
    public void rejectsBinarySetsAndInvalidSets() {
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.fromPlainValue("empty", Collections.emptySet()));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.fromPlainValue("binarySet", Collections.singleton(new byte[]{1})));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.fromPlainValue("mixed", new LinkedHashSet<>(Arrays.asList("Argentina", 10))));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.fromPlainValue("withNull", Collections.singleton(null)));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.fromPlainValue("nan", Collections.singleton(Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.fromPlainValue("flag", Collections.singletonMap("image", new byte[]{1})));
    }

    @Test
    public void rejectsNullDtoOrEmptySetInToPlainValue() {
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.toPlainValue(null));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.toPlainValue(new DynamoDbAttributeValueDto()));

        DynamoDbAttributeValueDto emptySet = new DynamoDbAttributeValueDto();
        emptySet.attributeName = "countries";
        emptySet.type = DynamoDbValueTypeDto.STRING_SET;
        emptySet.setValue = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDtoUtils.toPlainValue(emptySet));
    }
}
