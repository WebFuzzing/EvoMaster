package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.List;
import java.util.stream.Collectors;

/**
 * created by manzhang on 2021/11/3
 */
public class ObjectParamSchema extends ParamSchema{
    private final List<ParamSchema> fields;

    public ObjectParamSchema(String type, String fullTypeName, String name, List<ParamSchema> fields) {
        super(type, fullTypeName, name);
        this.fields = fields;
    }

    @Override
    public ParamSchema copy() {
        return new ObjectParamSchema(getType(), getFullTypeName(), getName(), fields.stream().map(ParamSchema::copy).collect(Collectors.toList()));
    }

    public List<ParamSchema> getFields() {
        return fields;
    }

}
