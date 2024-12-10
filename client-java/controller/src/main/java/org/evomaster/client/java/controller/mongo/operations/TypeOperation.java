package org.evomaster.client.java.controller.mongo.operations;


/**
 * Represent $type operation.
 * Selects documents where the value of the field is an instance of the specified BSON type(s).
 */
public class TypeOperation extends QueryOperation {
    private final String fieldName;
    private final Object type;

    public TypeOperation(String fieldName, Object type) {
        this.fieldName = fieldName;
        this.type = type;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getType() {
        return type;
    }
}