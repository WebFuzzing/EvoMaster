package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.JavaDtoSpec;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.methodInvocation;

/**
 * boolean param
 */
public class BooleanParam extends PrimitiveOrWrapperParam<Boolean> {

    private final static String JAVA_PR_METHOD = "booleanValue";
    private final static String KOTLIN_PR_METHOD = "toBoolean";

    public BooleanParam(String name, String type, String fullTypeName, Class<?> clazz, AccessibleSchema accessibleSchema, JavaDtoSpec spec) {
        super(name, type, fullTypeName, clazz, accessibleSchema, spec);
    }

    public BooleanParam(String name, PrimitiveOrWrapperType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        if (getValue() == null)
            return null;
        return String.valueOf(getValue());
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getValue() != null)
            dto.stringValue = getValue().toString();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.BOOLEAN;
        else
            dto.type.type = RPCSupportedDataType.P_BOOLEAN;
        return dto;
    }

    @Override
    public BooleanParam copyStructure() {
        return new BooleanParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public void setValueBasedOnStringValue(String stringValue) {
        try {
            if (stringValue != null)
                setValue(Boolean.parseBoolean(stringValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+stringValue +" as boolean value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Boolean) instance);
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return instance instanceof Boolean;
    }



    @Override
    public String primitiveValueMethod(boolean isJava) {
        if (isJava) return JAVA_PR_METHOD;
        return KOTLIN_PR_METHOD;
    }
}
