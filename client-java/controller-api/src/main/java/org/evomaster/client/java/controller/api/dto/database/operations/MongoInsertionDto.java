package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

public class MongoInsertionDto {

    public String databaseName;

    public String collectionName;

    public List<MongoInsertionEntryDto> data = new ArrayList<>();
}
