package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * created by manzhang on 2021/11/15
 */
public class LongParam extends PrimitiveOrWrapperParam<Long> {
    public LongParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public LongParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }
}
