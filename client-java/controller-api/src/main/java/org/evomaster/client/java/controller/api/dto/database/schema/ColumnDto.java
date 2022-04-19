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
     * scale of number
     * a negative number means that the scale is unspecified or inapplicable
     */
    public int scale = -1;

}
