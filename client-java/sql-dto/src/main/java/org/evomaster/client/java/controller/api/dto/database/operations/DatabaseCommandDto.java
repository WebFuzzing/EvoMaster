package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

public class DatabaseCommandDto {

    /**
     * Optional numeric id. If present, the driver will keep track of the last received, and expect
     * these id to be in ascending order. If they are not, the commands will be discarded.
     *
     * This was introduced to avoid side-effects of POST commands being repeated.
     * In "theory", it should never happen, as POST is not idempotent. But libraries like
     * Jersey have such major bug.
     */
    public Integer idCounter;

    /**
     * A generic SQL command.
     * Must be null if "insertions" field != null
     */
    public String command;

    /**
     * One or more insertion operation via SQL.
     * Must be null if "command" field != null.
     * Ids must be unique, but no need to be in any
     * specific order.
     * However, an insertion X referring to Y should
     * come in this list AFTER Y.
     */
    public List<InsertionDto> insertions = new ArrayList<>();
}
