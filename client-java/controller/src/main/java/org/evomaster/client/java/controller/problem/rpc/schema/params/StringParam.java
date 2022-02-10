package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.StringType;

import java.util.Collections;
import java.util.List;

/**
 * string param
 */
public class StringParam extends NamedTypedValue<StringType, String> {

    /**
     * min length of the string
     */
    private Integer minSize;

    /**
     * max length of the string
     */
    private Integer maxSize;

    /**
     * min value of the string
     * note that a string might be specified with its min value, eg, representing sth like UUID
     * then we still need to collect such info
     * if a string has such info, when init gene, we will add a specification as LongGene for it
     */
    private Long min;

    /**
     * max value of the string
     * note that a string might be specified with its max value, eg, representing sth like UUID
     * then we still need to collect such info
     * if a string has such info, when init gene, we will add a specification as LongGene for it
     */
    private Long max;

    public StringParam(String name, StringType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    public StringParam(String name, AccessibleSchema accessibleSchema) {
        super(name, new StringType(), accessibleSchema);
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
        return new StringParam(getName(), getType(),accessibleSchema);
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

        String code;
        if (accessibleSchema == null || accessibleSchema.isAccessible)
            code = CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, getType().getFullTypeName(), variableName, getValueAsJavaString());
        else{
            if (accessibleSchema.setterMethodName == null)
                throw new IllegalStateException("Error: private field, but there is no setter method");
            code = CodeJavaGenerator.oneLineSetterInstance(accessibleSchema.setterMethodName, getType().getFullTypeName(), variableName, getValueAsJavaString());
        }
        return Collections.singletonList(CodeJavaGenerator.getIndent(indent)+ code);
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName, int maxAssertionForDataInCollection) {
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
        return getValue() == null? null:"\""+CodeJavaGenerator.handleEscapeCharInString(getValue())+"\"";
    }

    @Override
    public void copyProperties(NamedTypedValue copy) {
        super.copyProperties(copy);
        if (copy instanceof StringParam){
            ((StringParam)copy).setMax(max);
            ((StringParam)copy).setMin(min);
            ((StringParam)copy).setMinSize(minSize);
            ((StringParam)copy).setMinSize(minSize);
        }
    }
}
