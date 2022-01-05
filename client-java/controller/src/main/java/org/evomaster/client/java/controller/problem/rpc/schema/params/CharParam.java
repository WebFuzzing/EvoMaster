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
        if (getValue() != null)
            dto.stringValue = getValue().toString();
        return dto;
    }

    @Override
    public String getValueAsJavaString() {
        if (getValue() == null)
            return null;
        return "'"+getValue()+"'";
    }

    @Override
    public CharParam copyStructure() {
        return new CharParam(getName(), getType());
    }


    @Override
    public void setValueBasedOnStringValue(String stringValue) {
        if (stringValue == null)
            return;
        if (stringValue.length() > 1){
            throw new RuntimeException("ERROR: a length of a char with its string value is more than 1, i.e., "+ stringValue.length());
        } else if (stringValue.length() == 1){
            setValue(stringValue.charAt(0));
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Character) instance);
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return instance instanceof Character;
    }
}
