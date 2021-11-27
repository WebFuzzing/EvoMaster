package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
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

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.CHAR;
        else
            dto.type.type = RPCSupportedDataType.P_CHAR;
        return dto;
    }

    @Override
    public CharParam copyStructure() {
        return new CharParam(getName(), getType());
    }
}
