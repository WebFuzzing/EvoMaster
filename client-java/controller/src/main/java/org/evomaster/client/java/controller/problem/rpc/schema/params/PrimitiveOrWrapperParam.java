package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * Primitive types Param
 */
public abstract class PrimitiveOrWrapperParam<V> extends NamedTypedValue<PrimitiveOrWrapperType, V> {

    /**
     * min value if it is specified
     */
    private Long min;

    /**
     * max value of it is specified
     */
    private Long max;

    public PrimitiveOrWrapperParam(String name, String type, String fullTypeName, Class<?> clazz, AccessibleSchema accessibleSchema){
        this(name, new PrimitiveOrWrapperType(type, fullTypeName, clazz), accessibleSchema);
    }

    public PrimitiveOrWrapperParam(String name, PrimitiveOrWrapperType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
        // primitive type is not nullable
        setNullable(getType().isWrapper);
    }

    public static PrimitiveOrWrapperParam build(String name, Class<?> clazz, AccessibleSchema accessibleSchema){
        if (clazz == Integer.class || clazz == int.class)
            return new IntParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema);
        if (clazz == Boolean.class || clazz == boolean.class)
            return new BooleanParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema);
        if (clazz == Double.class || clazz == double.class)
            return new DoubleParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema);
        if (clazz == Float.class || clazz == float.class)
            return new FloatParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema);
        if (clazz == Long.class || clazz == long.class)
            return new LongParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema);
        if (clazz == Character.class || clazz == char.class)
            return new CharParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema);
        if (clazz == Byte.class || clazz == byte.class)
            return new ByteParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema);
        if (clazz == Short.class || clazz == short.class)
            return new ShortParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema);
        throw new RuntimeException("PrimitiveOrWrapperParam: unhandled type "+ clazz.getName());
    }

    /**
     * setter might not use exact same type for primitive type
     * @param type
     * @return
     */
    public static Type getPrimitiveOrWrapper(Type type){
        if (Integer.class.equals(type)) {
            return int.class;
        } else if (int.class.equals(type)) {
            return Integer.class;
        } else if (Boolean.class.equals(type)) {
            return boolean.class;
        } else if (boolean.class.equals(type)) {
            return Boolean.class;
        } else if (Double.class.equals(type)) {
            return double.class;
        } else if (double.class.equals(type)) {
            return Double.class;
        } else if (Float.class.equals(type)) {
            return float.class;
        } else if (float.class.equals(type)) {
            return Float.class;
        } else if (Long.class.equals(type)) {
            return long.class;
        } else if (long.class.equals(type)) {
            return Long.class;
        } else if (Character.class.equals(type)) {
            return char.class;
        } else if (char.class.equals(type)) {
            return Character.class;
        } else if (Byte.class.equals(type)) {
            return byte.class;
        } else if (byte.class.equals(type)) {
            return Byte.class;
        } else if (Short.class.equals(type)) {
            return short.class;
        } else if (short.class.equals(type)) {
            return Short.class;
        }
        return type;
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.minValue = min;
        dto.maxValue = max;
        return dto;
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
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        String code;
        if (accessibleSchema == null || accessibleSchema.isAccessible){
            if (getType().isWrapper || getValue() != null){
                code = CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, getType().getFullTypeName(), variableName, getValueAsJavaString());
            }else{
                // ignore instance of primitive types if the value is not assigned
                return Collections.emptyList();
            }
        } else{
            if (accessibleSchema.setterMethodName == null)
                throw new IllegalStateException("Error: private field, but there is no setter method");
            code = CodeJavaGenerator.oneLineSetterInstance(accessibleSchema.setterMethodName, null, variableName, getValueAsJavaString());
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
            sb.append(CodeJavaGenerator.junitAssertEquals(getValueAsJavaString(), getPrimitiveValue(responseVarName)));

        return Collections.singletonList(sb.toString());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        setValueBasedOnStringValue(dto.stringValue);
    }

    abstract public void setValueBasedOnStringValue(String stringValue);

    /**
     *
     * @param responseVarName refers to the variable name in response
     * @return a string to get its primitive value if the param is Wrapper class
     *          eg, res.byteValue() for byte with a response variable name res
     */
    abstract public String getPrimitiveValue(String responseVarName);

    @Override
    public void copyProperties(NamedTypedValue copy) {
        super.copyProperties(copy);
        if (copy instanceof PrimitiveOrWrapperParam){
            ((PrimitiveOrWrapperParam)copy).setMin(min);
            ((PrimitiveOrWrapperParam)copy).setMax(max);
        }
    }
}
