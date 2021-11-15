package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * created by manzhang on 2021/11/15
 */
public class FloatParam extends PrimitiveOrWrapperParam<Float> {
    public FloatParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public FloatParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }
}
