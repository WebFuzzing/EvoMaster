package org.evomaster.client.java.controller.api.dto.database.operations;

/**
 * Contains data to be inserted into Redis.
 */
public class RedisInsertionDto {

    /** The Redis command.*/
    public String command;

    /** The Redis key.*/
    public String key;

    /** The field associated to the value. */
    public String field;

    /** The serialized value to set for that key. */
    public String value;
}
