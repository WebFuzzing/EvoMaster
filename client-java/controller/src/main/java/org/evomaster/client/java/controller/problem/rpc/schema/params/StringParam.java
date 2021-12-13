package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.StringType;

import java.util.Collections;
import java.util.List;

/**
 * string param
 */
public class StringParam extends NamedTypedValue<StringType, String> {

    public StringParam(String name) {
        super(name, new StringType());
    }

    @Override
    public Object newInstance() {
        return getValue();
    }

    @Override
    public StringParam copyStructure() {
        return new StringParam(getName());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.jsonValue != null)
            setValue(dto.jsonValue);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getValue() != null)
            dto.jsonValue = getValue().toString();
        return dto;
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((String) instance);
    }

    @Override
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        String value = null;
        if (getValue() != null)
            value = "\""+getValue()+"\"";
        return Collections.singletonList(CodeJavaGenerator.getIndent(indent)+CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, getType().getFullTypeName(), variableName, value));
    }

}
