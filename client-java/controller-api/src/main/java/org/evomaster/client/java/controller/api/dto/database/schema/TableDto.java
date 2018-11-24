package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class TableDto {

    public String name;

    public List<ColumnDto> columns = new ArrayList<>();

    public List<ForeignKeyDto> foreignKeys = new ArrayList<>();

    public List<String> primaryKeySequence = new ArrayList<>();
}
