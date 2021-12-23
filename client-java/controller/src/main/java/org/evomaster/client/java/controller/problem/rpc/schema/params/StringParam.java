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

    private Integer minSize;
    private Integer maxSize;

    private Long min;
    private Long max;

    public StringParam(String name) {
        super(name, new StringType());
    }


    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        if (this.minSize != null && this.minSize >= minSize)
            return;
        this.minSize = minSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public Long getMin() {
        return min;
    }

    public void setMin(Long min) {
        this.min = min;
    }

    public Long getMax() {
        return max;
    }

    public void setMax(Long max) {
        this.max = max;
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
        if (dto.stringValue != null)
            setValue(dto.stringValue);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getValue() != null)
            dto.stringValue = getValue();
        if (maxSize != null)
            dto.maxSize = Long.valueOf(maxSize);
        if (minSize != null)
            dto.minSize = Long.valueOf(minSize);
        if (min != null)
            dto.minValue = min;
        if (max != null)
            dto.maxValue = max;
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
            value = getValueAsJavaString();
        return Collections.singletonList(CodeJavaGenerator.getIndent(indent)+CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, getType().getFullTypeName(), variableName, value));
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName) {
        StringBuilder sb = new StringBuilder();
        sb.append(CodeJavaGenerator.getIndent(indent));
        if (getValue() == null)
            sb.append(CodeJavaGenerator.junitAssertNull(responseVarName));
        else
            sb.append(CodeJavaGenerator.junitAssertEquals(getValueAsJavaString(), responseVarName));

        return Collections.singletonList(sb.toString());
    }

    @Override
    public String getValueAsJavaString() {
        return "\""+CodeJavaGenerator.handleEscapeCharInString(getValue())+"\"";
    }

}
