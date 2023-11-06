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


    /**
     * Order in which the columns in the primary keys are listed
     * in the schema.
     *
     * For example, the primary key (activity_1_id, activity_2_id) results
     * in the list "activity_1_id", "activity_2_id".
     *
     * If the primary key has only one column, this sequence has only one
     * element.
     */
    public List<String> primaryKeySequence = new ArrayList<>();


    /**
     * All constraints that are not directly supported
     */
    public List<TableCheckExpressionDto> tableCheckExpressions = new ArrayList<>();

}
