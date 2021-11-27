package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * long param
 */
public class LongParam extends PrimitiveOrWrapperParam<Long> {
    public LongParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public LongParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.LONG;
        else
            dto.type.type = RPCSupportedDataType.P_LONG;
        return dto;
    }
}
