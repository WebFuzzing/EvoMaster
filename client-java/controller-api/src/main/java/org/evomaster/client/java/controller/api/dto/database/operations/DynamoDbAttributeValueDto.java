package org.evomaster.client.java.controller.api.dto.database.operations;

/**
 * A named scalar DynamoDB attribute value.
 */
public class DynamoDbAttributeValueDto {

    public String attributeName;
    public DynamoDbScalarTypeDto type;
    public String value;

    public DynamoDbAttributeValueDto() {
    }

    /**
     * Creates an attribute value.
     *
     * @param attributeName attribute name
     * @param type DynamoDB scalar type
     * @param value string-preserved value
     */
    public DynamoDbAttributeValueDto(String attributeName, DynamoDbScalarTypeDto type, String value) {
        this.attributeName = attributeName;
        this.type = type;
        this.value = value;
    }
}
