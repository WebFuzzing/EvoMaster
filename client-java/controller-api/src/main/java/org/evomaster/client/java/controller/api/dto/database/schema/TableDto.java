package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class TableDto {

    /**
     * The name of the table
     */
    public String name;

    /**
     * A list of descriptions for each column in the table
     */
    public List<ColumnDto> columns = new ArrayList<>();

    /**
     * Constraints on the table for foreign keys, if any
     */
    public List<ForeignKeyDto> foreignKeys = new ArrayList<>();


    public List<String> primaryKeySequence = new ArrayList<>();
}
