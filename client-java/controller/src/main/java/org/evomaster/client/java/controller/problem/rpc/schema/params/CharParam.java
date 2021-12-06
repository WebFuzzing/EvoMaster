package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * char param
 */
public class CharParam extends PrimitiveOrWrapperParam<Character> {
    public CharParam(String name, String type, String fullTypeName, Class<?> clazz) {
        super(name, type, fullTypeName, clazz);
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

    @Override
    public void setValue(ParamDto dto) {
        if (dto.jsonValue == null)
            return;
        if (dto.jsonValue.length() > 1){
            throw new RuntimeException("ERROR: a length of a char with its string value is more than 1, i.e., "+ dto.jsonValue.length());
        } else if (dto.jsonValue.length() == 1){
            setValue(dto.jsonValue.charAt(0));
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Character) instance);
    }
}
