package org.evomaster.client.java.controller.mongo.operations;


import org.evomaster.client.java.controller.mongo.utils.BsonHelper;

import java.util.List;
import java.util.Objects;

/**
 * Represent $type operation.
 * Selects documents where the value of the field is an instance of the specified BSON type(s).
 */
public class TypeOperation extends QueryOperationWithFieldPath {
    private final List<Object> bsonTypes;

    public TypeOperation(String fieldName, List<Object> bsonTypes) {
        super(fieldName);
        Objects.requireNonNull(bsonTypes);
        if (bsonTypes.isEmpty()) {
            throw new IllegalArgumentException("BSON types list cannot be empty");
        }
        for (Object type : bsonTypes) {
            if (!BsonHelper.isBsonType(type)) {
                throw new IllegalArgumentException("Invalid BSON type: " + type);
            }
        }
        this.bsonTypes = bsonTypes;
    }

    public List<Object> getBsonTypes() {
        return bsonTypes;
    }
}
