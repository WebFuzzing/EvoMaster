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

        @SuppressWarnings("unused")
        public Boolean hasBs() {
            return true;
        }

        @SuppressWarnings("unused")
        public Collection<?> bs() {
            return bs;
        }
    }

    private static class FakeBinaryAttributeValue {
        private final Object b;

        private FakeBinaryAttributeValue(Object b) {
            this.b = b;
        }

        @SuppressWarnings("unused")
        public Object b() {
            return b;
        }
    }
}
