package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class ForeignKeyDto {

    public List<String> sourceColumns = new ArrayList<>();

    public String targetTable;

    //TODO likely ll need to handle targetColumns if we have multi-columns
}
