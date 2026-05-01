package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a foreign key relationship in a database schema.
 *
 * A foreign key establishes a connection between two database tables,
 * where one table (source) references the primary key or a unique column
 * in another table (target).
 *
 * This class captures the metadata for the foreign key, including the columns
 * involved in the relationship and the target table being referenced.
 */
public class ForeignKeyDto {

    /**
     * A list of column names in the source table of the foreign key relationship.
     *
     * These column names correspond to the columns in the source table
     * that are used in the foreign key constraint. They establish the
     * connection to the target table by referencing its primary key
     * or unique columns.
     *
     * The order of the columns in this list corresponds to the order
     * in which they are defined in the foreign key constraint.
     */
    public List<String> sourceColumns = new ArrayList<>();

    /**
     * The name of the target table in a foreign key relationship.
     *
     * This variable specifies the table being referenced by the foreign key.
     * The value corresponds to the physical name of the target table in the database.
     * The foreign key relationship indicates that a column or set of columns in
     * the source table references a column or set of columns (usually the primary key)
     * in the target table.
     */
    public String targetTable;

    /**
     * A list of column names in the target table of the foreign key relationship.
     *
     * These column names represent the columns in the target table
     * that are referenced by the foreign key constraint. They typically
     * refer to primary key columns or unique columns in the target table.
     *
     * The order of the columns in this list corresponds to the order
     * defined in the foreign key relationship, ensuring a one-to-one
     * mapping with the source columns.
     */
    public List<String> targetColumns = new ArrayList<>();
}
