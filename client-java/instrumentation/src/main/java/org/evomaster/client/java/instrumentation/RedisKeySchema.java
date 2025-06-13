package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Schema of Key Values.
 */
public class RedisKeySchema implements Serializable {
    public final String keyName;
    public final String schemaJson;

    public RedisKeySchema(String keyName, String schemaJson) {
        this.keyName = keyName;
        this.schemaJson = schemaJson;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisKeySchema that = (RedisKeySchema) o;
        return Objects.equals(keyName, that.keyName) && Objects.equals(schemaJson, that.schemaJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyName, schemaJson);
    }

    @Override
    public String toString() {
        return "RedisKeySchema{" +
                "keyName='" + keyName + '\'' +
                ", schemaJson='" + schemaJson + '\'' +
                '}';
    }
}
