package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.JavaDtoSpec;
import org.evomaster.client.java.controller.problem.rpc.schema.types.StringType;
import org.evomaster.client.java.utils.SimpleLogger;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;

/**
 * string param
 */
public class StringParam extends NamedTypedValue<StringType, String> implements NumericConstraintBase<BigDecimal> {

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
    private BigDecimal min;

    /**
     * max value of the string
     * note that a string might be specified with its max value, eg, representing sth like UUID
     * then we still need to collect such info
     * if a string has such info, when init gene, we will add a specification as LongGene for it
     */
    private BigDecimal max;

    /**
     * pattern specified with regular expression
     */
    private String pattern;


    private boolean minInclusive = true;

    private boolean maxInclusive = true;

    /**
     * constraints with precision if applicable
     */
    private Integer precision;

    /**
     * constraints with scale if applicable
     */
    private Integer scale;

    public StringParam(String name, StringType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    public StringParam(String name, AccessibleSchema accessibleSchema, JavaDtoSpec spec) {
        super(name, new StringType(spec), accessibleSchema);
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
        if (this.maxSize != null)
            this.maxSize = Math.min(this.maxSize, maxSize);
        else
            this.maxSize = maxSize;
    }

    @Override
    public BigDecimal getMin() {
        return min;
    }

    @Override
    public void setMin(BigDecimal min) {
        if (min == null) return;
        if (this.min == null || this.min.compareTo(min) < 0)
            this.min = min;
    }

    @Override
    public BigDecimal getMax() {
        return max;
    }

    @Override
    public void setMax(BigDecimal max) {
        if (max == null) return;
        if (this.max  == null || this.max.compareTo(max) > 0)
            this.max = max;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Object newInstance() {
        return getValue();
    }

    @Override
    public List<String> referenceTypes() {
        return null;
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
    public void setValueBasedOnInstanceOrJson(Object json) throws JsonProcessingException {
        if (json == null)  return;

        if (json instanceof Number){
            SimpleLogger.recordErrorMessage("Warning: invalid data type in json for StringParam, attempt to set the value with number value for param "+getName());
            setValue(json.toString());
        }else {
            if (!(json instanceof String))
                throw new IllegalArgumentException("Cannot set value for StringParam " + getName() + " with the type:"+json.getClass().getName());
            setValue((String) json);
        }

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
        if (pattern != null)
            dto.pattern = pattern;

        handleConstraintsInCopyDto(dto);

        return dto;
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((String) instance);
    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable) {

        String code;
        if (accessibleSchema != null && accessibleSchema.setterMethodName != null)
            code = oneLineSetterInstance(accessibleSchema.setterMethodName, null, variableName, getValueAsJavaString(isJava), isJava, isVariableNullable);
        else {
            if (accessibleSchema != null && !accessibleSchema.isAccessible)
                throw new IllegalStateException("Error: private field, but there is no setter method");
            code = oneLineInstance(isDeclaration, doesIncludeName, getType().getTypeNameForInstanceInJavaOrKotlin(isJava), variableName, getValueAsJavaString(isJava), isJava, isVariableNullable);

        }
        return Collections.singletonList(getIndent(indent)+ code);
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent(indent));
        if (getValue() == null)
            sb.append(junitAssertNull(responseVarName, isJava));
        else
            sb.append(junitAssertEquals(getValueAsJavaString(isJava), responseVarName, isJava));

        return Collections.singletonList(sb.toString());
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        return getValue() == null? null:"\""+ handleEscapeCharInString(getValue(), isJava)+"\"";
    }

    @Override
    public void copyProperties(NamedTypedValue copy) {
        super.copyProperties(copy);
        if (copy instanceof StringParam){
            ((StringParam)copy).setMax(max);
            ((StringParam)copy).setMin(min);
            ((StringParam)copy).setMinSize(minSize);
            ((StringParam)copy).setMinSize(minSize);
            ((StringParam)copy).setPattern(pattern);
        }

        handleConstraintsInCopy(copy);
    }

    @Override
    public boolean getMinInclusive() {
        return minInclusive;
    }

    @Override
    public void setMinInclusive(boolean inclusive) {
        this.minInclusive = inclusive;
    }

    @Override
    public boolean getMaxInclusive() {
        return maxInclusive;
    }

    @Override
    public void setMaxInclusive(boolean inclusive) {
        this.maxInclusive = inclusive;
    }

    @Override
    public Integer getPrecision() {
        return precision;
    }

    @Override
    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    @Override
    public Integer getScale() {
        return this.scale;
    }

    @Override
    public void setScale(Integer scale) {
        this.scale = scale;
    }
}
