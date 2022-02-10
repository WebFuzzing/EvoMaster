package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

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
