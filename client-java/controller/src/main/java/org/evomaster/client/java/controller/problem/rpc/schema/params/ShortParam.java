package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * int param
 */
public class ShortParam extends PrimitiveOrWrapperParam<Short> {

    public ShortParam(String name, String type, String fullTypeName, Class<?> clazz) {
        super(name, type, fullTypeName, clazz);
    }

    public ShortParam(String name, PrimitiveOrWrapperType type) {
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
            dto.type.type = RPCSupportedDataType.SHORT;
        else
            dto.type.type = RPCSupportedDataType.P_SHORT;
        if (getValue() != null)
            dto.stringValue = getValue().toString();

        return dto;
    }

    @Override
    public ShortParam copyStructure() {
        return new ShortParam(getName(), getType());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        try {
            if (dto.stringValue != null)
                setValue(Short.parseShort(dto.stringValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+dto.stringValue +" as short value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Short) instance);
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return instance instanceof Short;
    }
}
