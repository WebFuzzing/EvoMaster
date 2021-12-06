package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * float param
 */
public class FloatParam extends PrimitiveOrWrapperParam<Float> {
    public FloatParam(String name, String type, String fullTypeName, Class<?> clazz) {
        super(name, type, fullTypeName, clazz);
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
            if (dto.jsonValue != null)
                setValue(Float.parseFloat(dto.jsonValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+dto.jsonValue+" as float value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Float) instance);
    }
}
