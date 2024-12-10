package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.JavaDtoSpec;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

import java.util.ArrayList;
import java.util.List;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;

/**
 * double param
 */
public class DoubleParam extends PrimitiveOrWrapperParam<Double> {

    private final static String JAVA_PR_METHOD = "doubleValue";
    private final static String KOTLIN_PR_METHOD = "toDouble";
    boolean minInclusive;

    boolean maxInclusive;

    public DoubleParam(String name, String type, String fullTypeName, Class<?> clazz, AccessibleSchema accessibleSchema, JavaDtoSpec spec) {
        super(name, type, fullTypeName, clazz, accessibleSchema, spec);
    }

    public DoubleParam(String name, PrimitiveOrWrapperType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        if (getValue() == null)
            return null;
        return ""+getValue();
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.DOUBLE;
        else
            dto.type.type = RPCSupportedDataType.P_DOUBLE;
        if (getValue() != null)
            dto.stringValue = getValue().toString();
        return dto;
    }

    @Override
    public DoubleParam copyStructure() {
        return new DoubleParam(getName(), getType(), accessibleSchema);
    }


    @Override
    public void setValueBasedOnStringValue(String stringValue) {
        try {
            if (stringValue != null)
                setValue(Double.parseDouble(stringValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+stringValue +" as double value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Double) instance);
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return instance instanceof Double;
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        if (getValue() == null) return super.newAssertionWithJavaOrKotlin(indent, responseVarName, maxAssertionForDataInCollection, isJava);

        List<String> codes = new ArrayList<>();
        if ((getValue().isInfinite() || getValue().isNaN())){
            // here we just add comments for it
            addComment(codes, "// "+responseVarName+ " is "+getValueAsJavaString(isJava), indent);
        }else{
            addCode(codes, junitAssertNumbersMatch(getValueAsJavaString(isJava), responseVarName, isJava), indent);
        }
        return codes;
    }


    @Override
    public String primitiveValueMethod(boolean isJava) {
        if (isJava) return JAVA_PR_METHOD;
        return KOTLIN_PR_METHOD;
    }
}
