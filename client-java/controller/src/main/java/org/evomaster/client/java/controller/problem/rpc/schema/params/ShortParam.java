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
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.SHORT;
        else
            dto.type.type = RPCSupportedDataType.P_SHORT;
        return dto;
    }

    @Override
    public ShortParam copyStructure() {
        return new ShortParam(getName(), getType());
    }

    @Override
    public void setValue(ParamDto dto) {
        try {
            setValue(Short.parseShort(dto.jsonValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+dto.jsonValue+" as short value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Short) instance);
    }
}
