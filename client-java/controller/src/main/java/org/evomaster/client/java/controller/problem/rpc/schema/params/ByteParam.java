package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.JavaDtoSpec;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.methodInvocation;

/**
 * byte param
 */
public class ByteParam extends PrimitiveOrWrapperParam<Byte> {

    public final static String TYPE_NAME = byte.class.getSimpleName();
    public final static String FULL_TYPE_NAME = byte.class.getName();

    private final static String JAVA_PR_METHOD  = "byteValue";
    private final static String KOTLIN_PR_METHOD = "toByte";

    public ByteParam(String name, String type, String fullTypeName, Class<?> clazz, AccessibleSchema accessibleSchema, JavaDtoSpec spec) {
        super(name, type, fullTypeName, clazz, accessibleSchema, spec);
    }

    public ByteParam(String name, PrimitiveOrWrapperType type, AccessibleSchema accessibleSchema) {
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
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.BYTE;
        else
            dto.type.type = RPCSupportedDataType.P_BYTE;
        if (getValue() != null)
            dto.stringValue = getValue().toString();
        return dto;
    }

    @Override
    public ByteParam copyStructure() {
        return new ByteParam(getName(), getType(), accessibleSchema);
    }


    @Override
    public void setValueBasedOnStringValue(String stringValue) {
        try {
            if (stringValue != null)
                setValue(Byte.parseByte(stringValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+stringValue +" as byte value");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((Byte) instance);
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return instance instanceof Byte;
    }


    @Override
    public String getCastType() {
        return byte.class.getName();
    }

    @Override
    public boolean castValueWithSpecificMethod(boolean isJava) {
        return !isJava;
    }

    @Override
    public String castValueInTestGenerationIfNeeded(String stringValue, boolean isJava) {
        if (isJava)
            return super.castValueInTestGenerationIfNeeded(stringValue, true);
        return methodInvocation(String.format("(%s)", stringValue), primitiveValueMethod(false), "", false, isNullable(), true);
    }

    @Override
    public String primitiveValueMethod(boolean isJava) {
        if (isJava)
            return JAVA_PR_METHOD;
        return KOTLIN_PR_METHOD;
    }
}
