package org.evomaster.client.java.controller.api.dto.database.schema;

public class ColumnDto {

    public String table;

    public String name;

    public String type;

    public int size;

    public boolean primaryKey;

    public boolean nullable;

    public boolean unique;

    public boolean autoIncrement;

    public boolean foreignKeyToAutoIncrement = false;

    public boolean isUnsigned = false;

    public boolean isEnumeratedType = false;

    /**
     * From https://www.postgresql.org/docs/14/rowtypes.html
     * """
     * A composite type represents the structure of a row or record;
     * it is essentially just a list of field names and their data types.
     * PostgreSQL allows composite types to be used in many of the same
     * ways that simple types can be used.
     * For example, a column of a table can be declared to be of a composite type.
     * """
     * Example composite types:
     *
     * CREATE TYPE complex AS (
     *     r       double precision,
     *     i       double precision
     * );
     *
     * CREATE TYPE inventory_item AS (
     *     name            text,
     *     supplier_id     integer,
     *     price           numeric
     * );
     */

    public boolean isCompositeType = false;
    /**
     * precision of number
     * a negative number means that the precision is unspecified or inapplicable
     */
    public int precision = -1;

    /**
     * The number of dimensions for arrays, matrixs, etc.
     */
    public int numberOfDimensions = 0;
}
