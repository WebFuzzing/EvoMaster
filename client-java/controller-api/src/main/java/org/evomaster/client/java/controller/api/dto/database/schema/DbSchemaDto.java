package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class DbSchemaDto {

    public DatabaseType databaseType;

    public String name;

    public List<TableDto> tables = new ArrayList<>();

}
