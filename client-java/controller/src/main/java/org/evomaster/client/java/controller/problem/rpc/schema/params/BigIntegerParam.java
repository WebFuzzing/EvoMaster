package org.evomaster.client.java.controller.problem.rpc.schema.params;


import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.BigIntegerType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * info for JDK:
 *      BigInteger constructors and operations throw ArithmeticException when the result is
 *      out of the supported range of -2^Integer.MAX_VALUE (exclusive) to +2^Integer.MAX_VALUE (exclusive).
 */
public class BigIntegerParam extends NamedTypedValue<BigIntegerType, BigInteger> {

    private BigInteger min;

    private BigInteger max;

    public BigIntegerParam(String name, BigIntegerType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    public BigIntegerParam(String name, AccessibleSchema accessibleSchema){
        this(name, new BigIntegerType(), accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return getValue();
    }

    @Override
    public NamedTypedValue<BigIntegerType, BigInteger> copyStructure() {
        return new BigIntegerParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public void copyProperties(NamedTypedValue copy) {
        super.copyProperties(copy);
        if (copy instanceof BigIntegerParam){
            ((BigIntegerParam) copy).setMax(max);
            ((BigIntegerParam) copy).setMin(min);
        }
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.stringValue == null) setValue(null);
        setValue(new BigInteger(dto.stringValue));
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((BigInteger) instance);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (min != null)
            dto.minValue = min.toString();
        if (max != null)
            dto.maxValue = max.toString();

        return dto;
    }

    @Override
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        String typeName = getType().getTypeNameForInstance();

        List<String> codes = new ArrayList<>();
        boolean isNull = (getValue() == null);
        String var = CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, typeName, variableName, null);
        CodeJavaGenerator.addCode(codes, var, indent);
        if (isNull) return codes;

        CodeJavaGenerator.addCode(codes, "{", indent);
        CodeJavaGenerator.addCode(codes, CodeJavaGenerator.setInstance(variableName, CodeJavaGenerator.newObjectConsParams(typeName, getValueAsJavaString())), indent+1);
        CodeJavaGenerator.addCode(codes, "}", indent);

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
    public String getValueAsJavaString() {
        if (getValue() == null)
            return null;
        return ""+getValue().toString()+"";
    }

    public BigInteger getMin() {
        return min;
    }

    public void setMin(BigInteger min) {
        this.min = min;
    }

    public BigInteger getMax() {
        return max;
    }

    public void setMax(BigInteger max) {
        this.max = max;
    }
}
