package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.ObjectType;

import java.util.List;

/**
 * created by manzhang on 2021/11/3
 */
public class ObjectParam extends NamedTypedValue<ObjectType, Object> {

    private List<NamedTypedValue> fields;

    public ObjectParam(String name, ObjectType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        throw new IllegalStateException("NOT IMPLEMENTED");
    }
}
