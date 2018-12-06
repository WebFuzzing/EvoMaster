package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

public class DatabaseCommandDto {

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
