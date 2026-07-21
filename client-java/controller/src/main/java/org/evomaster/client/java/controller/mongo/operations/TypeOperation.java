package org.evomaster.client.java.controller.mongo.operations;


/**
 * Represent $type operation.
 * Selects documents where the value of the field is an instance of the specified BSON type(s).
 */
public class TypeOperation extends QueryOperationWithField {
    private final Object type;

    public TypeOperation(String fieldName, Object type) {
        super(fieldName);
        this.type = type;
    }

    public Object getType() {
        return type;
    }
}
