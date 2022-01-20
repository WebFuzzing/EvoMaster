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
     * precision of number
     * a negative number means that the precision is unspecified or inapplicable
     */
    public int precision = -1;

    /**
     * The number of dimensions for arrays, matrixs, etc.
     */
    public int numberOfDimensions = 0;
}
