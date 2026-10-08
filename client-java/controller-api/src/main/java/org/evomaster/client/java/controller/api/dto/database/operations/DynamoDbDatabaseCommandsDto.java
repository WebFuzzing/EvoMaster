package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * DynamoDB insertion commands sent to the controller.
 */
public class DynamoDbDatabaseCommandsDto {

    public List<DynamoDbInsertionDto> insertions = new ArrayList<>();
}
