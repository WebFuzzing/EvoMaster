package org.evomaster.client.java.controller.api.dto.database.operations;

public class RedisInsertionDto {

    /** The Redis key.*/
    public String key;

    /** The serialized value to set for that key. */
    public String value;

    /** Keyspace index, default 0. */
    public int keyspace = 0;
}
