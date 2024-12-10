package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $jsonSchema operation.
 * Matches documents that satisfy the specified JSON Schema.
 */
public class JsonSchemaOperation extends QueryOperation{
    private final Object schema;

    public JsonSchemaOperation(Object schema) {
        this.schema = schema;
    }

    public Object getSchema() {
        return schema;
    }
}