package org.evomaster.client.java.controller.api.dto.database.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisSearchFieldTypeTest {

    @Test
    void testSupportedTypes() {
        assertEquals(RedisSearchFieldType.TEXT, RedisSearchFieldType.fromRediSearch("TEXT"));
        assertEquals(RedisSearchFieldType.TAG, RedisSearchFieldType.fromRediSearch("TAG"));
        assertEquals(RedisSearchFieldType.NUMERIC, RedisSearchFieldType.fromRediSearch("NUMERIC"));
    }

    @Test
    void testCaseIsIgnored() {
        assertEquals(RedisSearchFieldType.TAG, RedisSearchFieldType.fromRediSearch("tag"));
        assertEquals(RedisSearchFieldType.NUMERIC, RedisSearchFieldType.fromRediSearch("Numeric"));
    }

    @Test
    void testUnsupportedTypesAreOther() {
        assertEquals(RedisSearchFieldType.OTHER, RedisSearchFieldType.fromRediSearch("GEO"));
        assertEquals(RedisSearchFieldType.OTHER, RedisSearchFieldType.fromRediSearch("VECTOR"));
        assertEquals(RedisSearchFieldType.OTHER, RedisSearchFieldType.fromRediSearch("GEOSHAPE"));
    }

    @Test
    void testNullAndBlankAreOther() {
        assertEquals(RedisSearchFieldType.OTHER, RedisSearchFieldType.fromRediSearch(null));
        assertEquals(RedisSearchFieldType.OTHER, RedisSearchFieldType.fromRediSearch(""));
    }
}
