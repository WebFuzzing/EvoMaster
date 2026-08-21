package org.evomaster.client.java.controller.dynamodb;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DynamoDbAttributeValueHelperTest {

    @Test
    public void testToPlainMapWithNonMapReturnsEmpty() {
        assertTrue(DynamoDbAttributeValueHelper.toPlainMap("world-cup").isEmpty());
    }

    @Test
    public void testToPlainMapConvertsKeysAndSkipsNullKeys() {
        Map<Object, Object> source = new LinkedHashMap<>();
        source.put(10, AttributeValue.builder().s("Lionel Messi").build());
        source.put(null, AttributeValue.builder().s("Kylian Mbappe").build());

        Map<String, Object> plain = DynamoDbAttributeValueHelper.toPlainMap(source);

        assertEquals(1, plain.size());
        assertEquals("Lionel Messi", plain.get("10"));
    }

    @Test
    public void testToPlainValueForNullMapAndCollection() {
        assertNull(DynamoDbAttributeValueHelper.toPlainValue(null));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("goals", AttributeValue.builder().n("7").build());
        assertEquals(7L, ((Map<?, ?>) DynamoDbAttributeValueHelper.toPlainValue(map)).get("goals"));

        List<Object> list = Arrays.asList(
                AttributeValue.builder().s("Argentina").build(),
                AttributeValue.builder().bool(true).build()
        );
        assertEquals(Arrays.asList("Argentina", true), DynamoDbAttributeValueHelper.toPlainValue(list));
    }

    @Test
    public void testToPlainValueWithNulHasPriority() {
        AttributeValue value = AttributeValue.builder().nul(true).s("Messi").n("36").bool(true).build();
        assertNull(DynamoDbAttributeValueHelper.toPlainValue(value));
    }

    @Test
    public void testToPlainValueWithNumberParsingVariants() {
        assertEquals(13L, DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().n("13").build()));
        assertEquals(1.75, (Double) DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().n("1.75").build()), 0.000001);
        assertEquals(30.0, (Double) DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().n("3e1").build()), 0.000001);

        Object invalid = DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().n("goals").build());
        assertInstanceOf(Double.class, invalid);
        assertTrue(Double.isNaN((Double) invalid));
    }

    @Test
    public void testToPlainValueWithEmptyNumberFallsBackToBool() {
        Object value = DynamoDbAttributeValueHelper.toPlainValue(new FakeAttributeValue("", true));
        assertEquals(true, value);
    }

    @Test
    public void testToPlainValueWithMapListAndSetShapes() {
        Map<String, AttributeValue> nested = new LinkedHashMap<>();
        nested.put("player", AttributeValue.builder().s("Mbappe").build());

        assertEquals(Collections.singletonMap("player", "Mbappe"),
                DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().m(nested).build()));

        List<AttributeValue> list = Arrays.asList(
                AttributeValue.builder().s("France").build(),
                AttributeValue.builder().n("8").build()
        );
        assertEquals(Arrays.asList("France", 8L),
                DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().l(list).build()));

        Object ss = DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().ss("Argentina", "Argentina", "France").build());
        assertEquals(new LinkedHashSet<>(Arrays.asList("Argentina", "France")), ss);

        Object ns = DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().ns("36", "7.5", "age?").build());
        assertInstanceOf(Set.class, ns);
        assertEquals(3, ((Set<?>) ns).size());
        assertTrue(((Set<?>) ns).contains(36L));
        assertTrue(((Set<?>) ns).contains(7.5));
        assertTrue(((Set<?>) ns).stream().anyMatch(v -> v instanceof Double && Double.isNaN((Double) v)));
    }

    @Test
    public void testToPlainValueWithBinaryAndBinarySet() {
        SdkBytes binary = SdkBytes.fromByteArray(new byte[]{1, 2, 3});
        Object single = DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().b(binary).build());
        assertEquals(binary, single);

        SdkBytes bsBinary = SdkBytes.fromByteArray(new byte[]{7, 8});
        Object bs = DynamoDbAttributeValueHelper.toPlainValue(AttributeValue.builder().bs(bsBinary).build());
        assertInstanceOf(Set.class, bs);
        assertEquals(1, ((Set<?>) bs).size());
        assertEquals(bsBinary, ((Set<?>) bs).iterator().next());
    }

    @Test
    public void testToPlainValueWithDirectByteBufferBinary() {
        Object converted = DynamoDbAttributeValueHelper.toPlainValue(new FakeBinaryAttributeValue(ByteBuffer.wrap(new byte[]{4, 5})));
        assertArrayEquals(new byte[]{4, 5}, (byte[]) converted);
    }

    @Test
    public void testToPlainValueWithBinarySetContainingNonBinary() {
        Object converted = DynamoDbAttributeValueHelper.toPlainValue(
                new FakeBinarySetAttributeValue(Arrays.asList(ByteBuffer.wrap(new byte[]{9}), "Brazil"))
        );

        assertInstanceOf(Set.class, converted);
        assertEquals(2, ((Set<?>) converted).size());
        Iterator<?> it = ((Set<?>) converted).iterator();
        assertArrayEquals(new byte[]{9}, (byte[]) it.next());
        assertEquals("Brazil", it.next());
    }

    @Test
    public void testToPlainValueFallbackWhenNoKnownShape() {
        Object marker = new Object();
        assertSame(marker, DynamoDbAttributeValueHelper.toPlainValue(marker));
    }

    /**
     * Verifies that document paths include every map field and list index in deterministic preorder,
     * while nulls, empty containers, and sets remain addressable leaves.
     */
    @Test
    public void testDocumentPathsAndLookupByPathForNestedMapsAndListIndexes() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("country", "Argentina");
        profile.put("clubs", Arrays.asList("Barcelona", "Inter Miami"));

        Map<String, Object> squad = new LinkedHashMap<>();
        squad.put("captain", "Lionel Messi");

        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", "Lionel Messi");
        player.put("profile", profile);
        player.put("squads", Collections.singletonList(squad));
        player.put("tournaments", Collections.singletonList(
                Collections.singletonList("FIFA World Cup")));
        player.put("retired", null);
        player.put("tags", new LinkedHashSet<>(Arrays.asList("forward", "captain")));
        player.put("emptyProfile", Collections.emptyMap());
        player.put("emptyMatches", Collections.emptyList());

        Set<String> paths = DynamoDbAttributeValueHelper.documentPaths(player);

        assertEquals(Arrays.asList(
                        "name",
                        "profile",
                        "profile.country",
                        "profile.clubs",
                        "profile.clubs[0]",
                        "profile.clubs[1]",
                        "squads",
                        "squads[0]",
                        "squads[0].captain",
                        "tournaments",
                        "tournaments[0]",
                        "tournaments[0][0]",
                        "retired",
                        "tags",
                        "emptyProfile",
                        "emptyMatches"),
                new ArrayList<>(paths));

        for (String path : paths) {
            assertTrue(DynamoDbAttributeValueHelper.lookupByPath(player, path).found, path);
        }
        assertSame(profile, DynamoDbAttributeValueHelper.lookupByPath(player, "profile").value);
        assertEquals("Argentina",
                DynamoDbAttributeValueHelper.lookupByPath(player, "profile.country").value);
        assertEquals("Inter Miami",
                DynamoDbAttributeValueHelper.lookupByPath(player, "profile.clubs[1]").value);
        assertEquals("Lionel Messi",
                DynamoDbAttributeValueHelper.lookupByPath(player, "squads[0].captain").value);
        assertEquals("FIFA World Cup",
                DynamoDbAttributeValueHelper.lookupByPath(player, "tournaments[0][0]").value);

        DynamoDbValueLookup explicitNull =
                DynamoDbAttributeValueHelper.lookupByPath(player, "retired");
        assertTrue(explicitNull.found);
        assertNull(explicitNull.value);
    }

    /**
     * Verifies that invalid inputs, absent fields, incompatible intermediate values, and invalid
     * list indexes all produce an unambiguous missing-path result.
     */
    @Test
    public void testLookupByPathRejectsInvalidTraversal() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("country", "Argentina");
        profile.put("clubs", Arrays.asList("Barcelona", "Inter Miami"));

        Map<String, Object> squad = Collections.singletonMap(
                "captain", "Lionel Messi");
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", "Lionel Messi");
        player.put("profile", profile);
        player.put("squads", Collections.singletonList(squad));

        List<DynamoDbValueLookup> missingLookups = Arrays.asList(
                DynamoDbAttributeValueHelper.lookupByPath(null, "name"),
                DynamoDbAttributeValueHelper.lookupByPath(player, null),
                DynamoDbAttributeValueHelper.lookupByPath(player, "  "),
                DynamoDbAttributeValueHelper.lookupByPath(player, "country"),
                DynamoDbAttributeValueHelper.lookupByPath(player, "name.first"),
                DynamoDbAttributeValueHelper.lookupByPath(player, "profile.country[0]"),
                DynamoDbAttributeValueHelper.lookupByPath(player, "profile.clubs[-1]"),
                DynamoDbAttributeValueHelper.lookupByPath(player, "profile.clubs[2]"),
                DynamoDbAttributeValueHelper.lookupByPath(player, "squads[0].coach"));

        for (DynamoDbValueLookup lookup : missingLookups) {
            assertFalse(lookup.found);
            assertNull(lookup.value);
        }
    }

    /**
     * Verifies empty-input handling and that path enumeration returns a snapshot independent of
     * later mutations to the source item.
     */
    @Test
    public void testDocumentPathsReturnsEmptySnapshotForMissingOrChangingItems() {
        assertTrue(DynamoDbAttributeValueHelper.documentPaths(null).isEmpty());
        assertTrue(DynamoDbAttributeValueHelper.documentPaths(Collections.emptyMap()).isEmpty());

        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", "Alexia Putellas");
        Set<String> paths = DynamoDbAttributeValueHelper.documentPaths(player);

        player.put("country", "Spain");
        assertEquals(Collections.singleton("name"), paths);
    }

    @Test
    public void testResolveAttributeTypeForPlainPlayerValues() {
        assertEquals(DynamoDbAttributeType.NULL,
                DynamoDbAttributeValueHelper.resolveAttributeType(null));
        assertEquals(DynamoDbAttributeType.STRING,
                DynamoDbAttributeValueHelper.resolveAttributeType("Lionel Messi"));
        assertEquals(DynamoDbAttributeType.NUMBER,
                DynamoDbAttributeValueHelper.resolveAttributeType(10L));
        assertEquals(DynamoDbAttributeType.BINARY,
                DynamoDbAttributeValueHelper.resolveAttributeType(new byte[]{1, 0}));
        assertEquals(DynamoDbAttributeType.BOOLEAN,
                DynamoDbAttributeValueHelper.resolveAttributeType(true));
        assertEquals(DynamoDbAttributeType.MAP,
                DynamoDbAttributeValueHelper.resolveAttributeType(Collections.singletonMap("country", "Argentina")));
        assertEquals(DynamoDbAttributeType.LIST,
                DynamoDbAttributeValueHelper.resolveAttributeType(Arrays.asList("Barcelona", "Paris")));
        assertEquals(DynamoDbAttributeType.STRING_SET,
                DynamoDbAttributeValueHelper.resolveAttributeType(
                        new LinkedHashSet<Object>(Arrays.asList("Argentina", "France"))));
        assertEquals(DynamoDbAttributeType.NUMBER_SET,
                DynamoDbAttributeValueHelper.resolveAttributeType(
                        new LinkedHashSet<Object>(Arrays.asList(10L, 7L))));
        assertEquals(DynamoDbAttributeType.BINARY_SET,
                DynamoDbAttributeValueHelper.resolveAttributeType(
                        new LinkedHashSet<Object>(Collections.singletonList(new byte[]{1}))));
    }

    @Test
    public void testResolveAttributeTypeFallbacks() {
        assertEquals(DynamoDbAttributeType.LIST,
                DynamoDbAttributeValueHelper.resolveAttributeType(Collections.emptySet()));
        assertEquals(DynamoDbAttributeType.LIST,
                DynamoDbAttributeValueHelper.resolveAttributeType(
                        new LinkedHashSet<>(Collections.singletonList(new Object()))));
        assertEquals(DynamoDbAttributeType.STRING,
                DynamoDbAttributeValueHelper.resolveAttributeType(new Object()));
    }

    private static class FakeAttributeValue {
        private final String n;
        private final Boolean bool;

        private FakeAttributeValue(String n, Boolean bool) {
            this.n = n;
            this.bool = bool;
        }

        @SuppressWarnings("unused")
        public String n() {
            return n;
        }

        @SuppressWarnings("unused")
        public Boolean bool() {
            return bool;
        }
    }

    private static class FakeBinarySetAttributeValue {
        private final Collection<?> bs;

        private FakeBinarySetAttributeValue(Collection<?> bs) {
            this.bs = bs;
        }

        @SuppressWarnings("unused") //invoked by reflection
        public Boolean hasBs() {
            return true;
        }

        @SuppressWarnings("unused") //invoked by reflection
        public Collection<?> bs() {
            return bs;
        }
    }

    private static class FakeBinaryAttributeValue {
        private final Object b;

        private FakeBinaryAttributeValue(Object b) {
            this.b = b;
        }

        @SuppressWarnings("unused") //invoked by reflection
        public Object b() {
            return b;
        }
    }
}
