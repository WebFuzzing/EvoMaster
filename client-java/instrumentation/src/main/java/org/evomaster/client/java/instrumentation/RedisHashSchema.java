package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Schema of the hashes stored under a Redis keyspace, ie a {@code @RedisHash}-annotated class.
 */
public class RedisHashSchema implements Serializable {
    private final String keyspace;
    private final String hashSchema;

    public RedisHashSchema(String keyspace, String hashSchema) {
        this.keyspace = keyspace;
        this.hashSchema = hashSchema;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public String getHashSchema() {
        return hashSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisHashSchema that = (RedisHashSchema) o;
        return Objects.equals(keyspace, that.keyspace) && Objects.equals(hashSchema, that.hashSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyspace, hashSchema);
    }

    @Override
    public String toString() {
        return "RedisHashSchema{" +
                "keyspace='" + keyspace + '\'' +
                ", hashSchema='" + hashSchema + '\'' +
                '}';
    }
}
