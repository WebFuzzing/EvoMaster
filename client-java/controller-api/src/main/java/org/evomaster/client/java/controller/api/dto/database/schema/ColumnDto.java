package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.List;

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

    public Integer lowerBound = null;

    public Integer upperBound = null;

    /**
     * List of all the values that the column can take
     */
    public List<String> enumValuesAsStrings = null;


}
