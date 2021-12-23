package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * double param
 */
public class DoubleParam extends PrimitiveOrWrapperParam<Double> {
    public DoubleParam(String name, String type, String fullTypeName, Class<?> clazz) {
        super(name, type, fullTypeName, clazz);
    }

    public DoubleParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }

    @Override
    public String getValueAsJavaString() {
        if (getValue() == null)
            return null;
        return ""+getValue();
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.DOUBLE;
        else
            dto.type.type = RPCSupportedDataType.P_DOUBLE;
        if (getValue() != null)
            dto.stringValue = getValue().toString();
        return dto;
    }

    @Override
    public DoubleParam copyStructure() {
        return new DoubleParam(getName(), getType());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        try {
            if (dto.stringValue != null)
                setValue(Double.parseDouble(dto.stringValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+dto.stringValue +" as double value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Double) instance);
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return instance instanceof Double;
    }
}
