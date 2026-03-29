package org.evomaster.client.java.controller.redis.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.RedisInsertionDto;
import java.util.List;

public interface RedisSequenceDsl {

    /**
     * A SET operation on the Redis database.
     *
     * @param key   the key under which the value will be stored in Redis
     * @param value the string value to store
     * @return a statement object on which the sequence can be continued or closed
     */
    RedisStatementDsl set(String key, String value);

    /**
     * Build the DTOs (Data Transfer Object) from this DSL,
     * closing it (i.e., not usable any longer).
     *
     * @return a list of DTOs representing all the insertion Redis commands defined in this DSL.
     */
    List<RedisInsertionDto> dtos();
}
