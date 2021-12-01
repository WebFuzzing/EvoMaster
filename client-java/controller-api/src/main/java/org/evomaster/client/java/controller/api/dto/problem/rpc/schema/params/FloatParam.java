package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * float param
 */
public class FloatParam extends PrimitiveOrWrapperParam<Float> {
    public FloatParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public FloatParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.FLOAT;
        else
            dto.type.type = RPCSupportedDataType.P_FLOAT;
        return dto;
    }

    @Override
    public FloatParam copyStructure() {
        return new FloatParam(getName(), getType());
    }

    @Override
    public void setValue(ParamDto dto) {
        try {
            setValue(Float.parseFloat(dto.jsonValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+dto.jsonValue+" as float value");
        }
    }
}
