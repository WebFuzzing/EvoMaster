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

    /**
     * a test can include a sequence of insertion and test actions, and the action may exist among insertions
     *  e.g., insert a rowA in Table1, test action1, insert a rowB in Table2 (rwoB may refer to rowA i.e., FK).
     * In order to keep pk or fk ids map among those insertion, isFirst is used to present whether the insertions are first in a test.
     * if the insertion is first, the map should be cleared, otherwise keep tracking those pk or fk info.
     */
    public boolean isFirst = true;
}
