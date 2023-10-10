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
 * float param
 */
public class FloatParam extends PrimitiveOrWrapperParam<Float> {

    private final static String JAVA_PR_METHOD = "floatValue";
    private final static String KOTLIN_PR_METHOD = "toFloat";

    public FloatParam(String name, String type, String fullTypeName, Class<?> clazz, AccessibleSchema accessibleSchema, JavaDtoSpec spec) {
        super(name, type, fullTypeName, clazz, accessibleSchema, spec);
    }

    public FloatParam(String name, PrimitiveOrWrapperType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        if (getValue() == null)
            return null;
        return ""+getValue()+"f";
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.FLOAT;
        else
            dto.type.type = RPCSupportedDataType.P_FLOAT;

        if (getValue() != null)
            dto.stringValue = getValue().toString();
        return dto;
    }

    @Override
    public FloatParam copyStructure() {
        return new FloatParam(getName(), getType(), accessibleSchema);
    }


    @Override
    public void setValueBasedOnStringValue(String stringValue) {
        try {
            if (stringValue != null)
                setValue(Float.parseFloat(stringValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+stringValue +" as float value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Float) instance);
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return instance instanceof Float;
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
