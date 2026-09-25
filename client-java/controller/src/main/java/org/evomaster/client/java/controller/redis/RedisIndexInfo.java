package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.controller.api.dto.database.execution.RedisSearchFieldType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The parts of a FT.INFO reply relevant to building the candidate documents of a RediSearch index
 * and to generating new data for it.
 */
public class RedisIndexInfo {

    public static final String HASH_KEY_TYPE = "HASH";

    /**
     * The type of key this index covers, e.g. "HASH" or "JSON". Data generation only supports
     * HASH-backed indexes.
     */
    private final String keyType;

    /**
     * The key prefixes declared for the index. A document is a candidate for the index if its
     * key starts with any of these.
     */
    private final List<String> prefixes;

    /**
     * The index schema, in the order its fields were declared.
     * Key -> the name of an indexed hash field, e.g. "age" or "street".
     * Value -> that field's RediSearch type.
     */
    private final Map<String, RedisSearchFieldType> attributes;

    public RedisIndexInfo(String keyType, List<String> prefixes, Map<String, RedisSearchFieldType> attributes) {
        this.keyType = keyType;
        this.prefixes = Collections.unmodifiableList(prefixes);
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    public String getKeyType() {
        return keyType;
    }

    public boolean isHashIndex() {
        return HASH_KEY_TYPE.equalsIgnoreCase(keyType);
    }

    public List<String> getPrefixes() {
        return prefixes;
    }

    public Map<String, RedisSearchFieldType> getAttributes() {
        return attributes;
    }
}
