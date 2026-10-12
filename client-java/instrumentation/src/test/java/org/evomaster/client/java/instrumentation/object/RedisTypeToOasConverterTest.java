package org.evomaster.client.java.instrumentation.object;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RedisTypeToOasConverterTest {

    public static class DefaultThenRedis {
        public Date date;
    }

    public static class RedisThenDefault {
        public Date date;
    }

    public static class AllTypes {
        public Date date;
        public BigDecimal price;
        public BigInteger big;
        public Instant instant;
        public UUID uuid;
    }

    public static class PlainOnly {
        public Instant instant;
    }

    private static String redisSchema(Class<?> klass) {
        return ClassToSchema.getOrDeriveSchemaWithItsRef(klass, false, RedisTypeToOasConverter.defaults());
    }

    @Test
    public void testDateIsEpochMillisecondsForRedis() {
        String schema = redisSchema(AllTypes.class);

        assertTrue(schema.contains("\"date\":{\"type\":\"integer\", \"format\":\"int64\", \"minimum\":0}"), schema);
    }

    @Test
    public void testBigDecimalIsADecimalNumberForRedis() {
        String schema = redisSchema(AllTypes.class);

        assertTrue(schema.contains("\"price\":{\"type\":\"number\", \"format\":\"decimal\"}"), schema);
    }

    @Test
    public void testBigIntegerIsAnIntegerForRedis() {
        String schema = redisSchema(AllTypes.class);

        assertTrue(schema.contains("\"big\":{\"type\":\"integer\", \"format\":\"int64\"}"), schema);
    }

    @Test
    public void testInstantAndUuidAreStringsForRedisAndNotObjects() {
        String schema = redisSchema(AllTypes.class);

        assertTrue(schema.contains("\"instant\":{\"type\":\"string\", \"format\":\"date-time\"}"), schema);
        assertTrue(schema.contains("\"uuid\":{\"type\":\"string\", \"format\":\"uuid\"}"), schema);
        assertFalse(schema.contains("mostSigBits"), schema);
        assertFalse(schema.contains("nanos"), schema);
    }

    @Test
    public void testWithoutConvertersInstantIsStillDerivedAsAnObject() {
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(PlainOnly.class);

        assertTrue(schema.contains("java.time.Instant"), schema);
        assertTrue(schema.contains("nanos"), schema);
    }

    @Test
    public void testConverterTakesPrecedenceOverTheBuiltInSchemaOfThatType() {
        // without converters a Date is a "date" formatted string, which Redis could not read back
        String plain = ClassToSchema.getOrDeriveNamedSchema(AllTypes.class, new ArrayList<>(), false, Collections.emptyList());
        String redis = ClassToSchema.getOrDeriveNamedSchema(AllTypes.class, new ArrayList<>(), false, RedisTypeToOasConverter.defaults());

        assertTrue(plain.contains("\"date\":{\"type\":\"string\", \"format\":\"date\"}"), plain);
        assertTrue(redis.contains("\"date\":{\"type\":\"integer\""), redis);
    }

    @Test
    public void testSameClassDerivedWithoutConvertersFirstDoesNotPoisonTheRedisSchema() {
        String plain = ClassToSchema.getOrDeriveSchemaWithItsRef(DefaultThenRedis.class);
        String redis = redisSchema(DefaultThenRedis.class);

        assertTrue(plain.contains("\"format\":\"date\""), plain);
        assertTrue(redis.contains("\"format\":\"int64\""), redis);
    }

    @Test
    public void testSameClassDerivedForRedisFirstDoesNotPoisonThePlainSchema() {
        String redis = redisSchema(RedisThenDefault.class);
        String plain = ClassToSchema.getOrDeriveSchemaWithItsRef(RedisThenDefault.class);

        assertTrue(redis.contains("\"format\":\"int64\""), redis);
        assertTrue(plain.contains("\"format\":\"date\""), plain);
    }

    @Test
    public void testSchemaForRedisIsCachedAcrossCallsEvenThoughConvertersAreNewInstances() {
        String first = redisSchema(AllTypes.class);
        String second = redisSchema(AllTypes.class);

        assertSame(first, second);
    }
}
