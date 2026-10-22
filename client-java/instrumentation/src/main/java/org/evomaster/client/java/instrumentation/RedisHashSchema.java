package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Schema of the hashes stored under a Redis keyspace, ie a {@code @RedisHash}-annotated class.
 */
public class RedisHashSchema implements Serializable {
    private final String keyspace;
    private final String hashSchema;
    /**
     * Name of the property that holds the entity's id, or null if it could not be determined.
     * Spring Data Redis writes it as one more field of the hash and, when reading, takes the
     * entity's id from that field and not from the key.
     */
    private final String idField;

    public RedisHashSchema(String keyspace, String hashSchema) {
        this(keyspace, hashSchema, null);
    }

    public RedisHashSchema(String keyspace, String hashSchema, String idField) {
        this.keyspace = keyspace;
        this.hashSchema = hashSchema;
        this.idField = idField;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public String getHashSchema() {
        return hashSchema;
    }

    public String getIdField() {
        return idField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisHashSchema that = (RedisHashSchema) o;
        return Objects.equals(keyspace, that.keyspace)
                && Objects.equals(hashSchema, that.hashSchema)
                && Objects.equals(idField, that.idField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyspace, hashSchema, idField);
    }

    @Override
    public String toString() {
        return "RedisHashSchema{" +
                "keyspace='" + keyspace + '\'' +
                ", hashSchema='" + hashSchema + '\'' +
                ", idField='" + idField + '\'' +
                '}';
    }
}
