package org.evomaster.clientJava.controllerApi.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class TableDto {

    public String name;

    public List<ColumnDto> columns = new ArrayList<>();

    public List<ForeignKeyDto> foreignKeys = new ArrayList<>();
}
