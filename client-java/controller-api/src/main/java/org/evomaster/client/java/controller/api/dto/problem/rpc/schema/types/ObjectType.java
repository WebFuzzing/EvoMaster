package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

import java.util.List;

/**
 * created by manzhang on 2021/11/15
 */
public class ObjectType extends TypeSchema {
    private final List<NamedTypedValue> fields;

    public ObjectType(String type, String fullTypeName, List<NamedTypedValue> fields) {
        super(type, fullTypeName);
        this.fields = fields;
    }

    public List<NamedTypedValue> getFields() {
        return fields;
    }
}
