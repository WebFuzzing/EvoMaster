package org.evomaster.clientJava.controllerApi.dto.database;

import java.util.ArrayList;
import java.util.List;

public class ForeignKeyDto {

    public List<String> columns = new ArrayList<>();

    public String targetTable;
}
