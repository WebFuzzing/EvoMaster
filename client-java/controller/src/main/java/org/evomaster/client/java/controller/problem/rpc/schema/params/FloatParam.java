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
    public String getValueAsJavaString() {
        if (getValue() == null)
            return null;
        return ""+getValue()+"f";
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.FLOAT;
        else
            dto.type.type = RPCSupportedDataType.P_FLOAT;

        if (getValue() != null)
            dto.stringValue = getValue().toString();
        return dto;
    }

    @Override
    public FloatParam copyStructure() {
        return new FloatParam(getName(), getType());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        try {
            if (dto.stringValue != null)
                setValue(Float.parseFloat(dto.stringValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+dto.stringValue +" as float value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Float) instance);
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return instance instanceof Float;
    }
}
