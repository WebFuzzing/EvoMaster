package org.evomaster.client.java.instrumentation.object;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Java type the way Spring Data Redis stores it in a hash field, rather than the way a
 * JSON body would carry it: each hash field is a plain string that the SUT parses back into the
 * property's type, so the generated value must be one that parse accepts.
 * <p>
 * For example, a {@link Date} is stored as epoch milliseconds, and a "2024-01-02" string (the
 * default JSON representation) makes reading the entity fail.
 */
public class RedisTypeToOasConverter extends CustomTypeToOasConverter {

    private final Class<?> type;
    private final String schema;

    private RedisTypeToOasConverter(Class<?> type, String schema) {
        this.type = type;
        this.schema = schema;
    }

    /**
     * @return one converter for each type whose hash representation differs from its default schema
     */
    public static List<CustomTypeToOasConverter> defaults() {
        return Collections.unmodifiableList(Arrays.asList(
                // stored as epoch milliseconds
                new RedisTypeToOasConverter(Date.class, "{\"type\":\"integer\", \"format\":\"int64\", \"minimum\":0}"),
                // stored as its decimal string; the default schema would only ever produce integers
                new RedisTypeToOasConverter(BigDecimal.class, "{\"type\":\"number\", \"format\":\"decimal\"}"),
                new RedisTypeToOasConverter(BigInteger.class, "{\"type\":\"integer\", \"format\":\"int64\"}"),
                // stored as ISO-8601 strings; without a converter they would be seen as objects
                new RedisTypeToOasConverter(Instant.class, "{\"type\":\"string\", \"format\":\"date-time\"}"),
                new RedisTypeToOasConverter(UUID.class, "{\"type\":\"string\", \"format\":\"uuid\"}")
        ));
    }

    @Override
    public String convert() {
        return schema;
    }

    @Override
    public boolean isInstanceOf(Class<?> klass) {
        return type.isAssignableFrom(klass);
    }

    @Override
    public String cacheId() {
        return getClass().getName() + ":" + type.getName();
    }
}
