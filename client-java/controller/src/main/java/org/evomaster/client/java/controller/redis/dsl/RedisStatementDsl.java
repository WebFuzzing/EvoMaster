package org.evomaster.client.java.controller.redis.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.RedisInsertionDto;
import java.util.List;

public interface RedisStatementDsl {

    /**
     * Close the current statement and return to the sequence,
     * allowing further Redis commands to be chained.
     *
     * @return the sequence object on which new Redis commands can be added
     */
    RedisSequenceDsl and();

    /**
     * Build the DTOs (Data Transfer Object) from this DSL,
     * closing it (i.e., not usable any longer).
     *
     * @return a list of DTOs representing all the insertion Redis commands defined in this DSL.
     */
    List<RedisInsertionDto> dtos();
}