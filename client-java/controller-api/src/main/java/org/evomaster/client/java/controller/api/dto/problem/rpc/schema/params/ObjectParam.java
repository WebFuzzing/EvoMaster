package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.ObjectType;

import java.util.List;

/**
 * created by manzhang on 2021/11/3
 */
public class ObjectParam extends NamedTypedValue<ObjectType, Object> {

    private List<NamedTypedValue> fields;

    public ObjectParam(String name, ObjectType type, List<NamedTypedValue> fields) {
        super(name, type);
        this.fields = fields;
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return null;
    }
}
