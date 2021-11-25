package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * char param
 */
public class CharParam extends PrimitiveOrWrapperParam<Character> {
    public CharParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public CharParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }
}
