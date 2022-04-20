package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.BigDecimalType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator.*;

public class BigDecimalParam extends NamedTypedValue<BigDecimalType, BigDecimal>{

    private BigDecimal min;

    private BigDecimal max;

    private Integer precision;

    private Integer scale;

    public BigDecimalParam(String name, BigDecimalType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    public BigDecimalParam(String name, AccessibleSchema accessibleSchema){
        this(name, new BigDecimalType(), accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return getValue();
    }

    @Override
    public NamedTypedValue<BigDecimalType, BigDecimal> copyStructure() {
        return new BigDecimalParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public void copyProperties(NamedTypedValue copy) {
        super.copyProperties(copy);
        if (copy instanceof BigDecimalParam){
            ((BigDecimalParam) copy).setMax(max);
            ((BigDecimalParam) copy).setMin(min);
            ((BigDecimalParam) copy).setPrecision(precision);
            ((BigDecimalParam) copy).setScale(scale);
        }
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        MathContext mc = null;
        BigDecimal bd = null;
        if (precision == null)
            bd = new BigDecimal(dto.stringValue);
        else {
            mc = new MathContext(precision);
            bd = new BigDecimal(dto.stringValue, mc);
        }

        if (scale != null)
            bd = bd.setScale(scale, RoundingMode.HALF_UP);

        setValue(bd);
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((BigDecimal) instance);
    }

    @Override
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        String typeName = getType().getTypeNameForInstance();

        List<String> codes = new ArrayList<>();
        boolean isNull = (getValue() == null);
        String var = oneLineInstance(isDeclaration, doesIncludeName, typeName, variableName, null);
        addCode(codes, var, indent);
        if (isNull) return codes;

        addCode(codes, "{", indent);
        String mcVar = variableName + "_mc";
        String consParam = getValueAsJavaString();
        if (precision != null){
            addCode(codes, oneLineInstance(true, true, MathContext.class.getName(), mcVar,
                    newObjectConsParams(MathContext.class.getName(), precision.toString())), indent+1);
            consParam += ", "+mcVar;
        }
        addCode(codes, setInstance(variableName, newObjectConsParams(typeName, consParam)), indent+1);
        if (scale != null){
            addCode(codes, oneLineSetterInstance("setScale", null, variableName, scale+", "+RoundingMode.class.getName()+".HALF_UP"), indent+1);
        }

        addCode(codes, "}", indent);

        return codes;
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName, int maxAssertionForDataInCollection) {
        // assertion with its string representation
        StringBuilder sb = new StringBuilder();
        sb.append(CodeJavaGenerator.getIndent(indent));
        if (getValue() == null)
            sb.append(CodeJavaGenerator.junitAssertNull(responseVarName));
        else
            sb.append(CodeJavaGenerator.junitAssertEquals(getValueAsJavaString(), responseVarName+".toString()"));

        return Collections.singletonList(sb.toString());
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (max != null)
            dto.maxValue = max.toString();
        if (min != null)
            dto.minValue = min.toString();
        dto.precision = precision;
        dto.scale = scale;
        return dto;
    }

    @Override
    public String getValueAsJavaString() {
        if (getValue() == null)
            return null;
        return ""+getValue().toString()+"";
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }
}
