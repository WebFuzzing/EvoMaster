package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * double param
 */
public class DoubleParam extends PrimitiveOrWrapperParam<Double> {
    public DoubleParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public DoubleParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.DOUBLE;
        else
            dto.type.type = RPCSupportedDataType.P_DOUBLE;
        return dto;
    }
}
