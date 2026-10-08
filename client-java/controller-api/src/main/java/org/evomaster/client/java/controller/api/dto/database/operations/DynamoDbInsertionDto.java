package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * An item to insert into a DynamoDB table.
 */
public class DynamoDbInsertionDto {

    public String tableName;
    public List<DynamoDbAttributeValueDto> attributes = new ArrayList<>();
}
