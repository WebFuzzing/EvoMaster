package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.JavaDtoSpec;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.methodInvocation;

/**
 * Primitive types Param
 */
public abstract class PrimitiveOrWrapperParam<V> extends NamedTypedValue<PrimitiveOrWrapperType, V> implements NumericConstraintBase<BigDecimal> {

    /**
     * min value if it is specified
     */
    private BigDecimal min;

    /**
     * max value of it is specified
     */
    private BigDecimal max;

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

    public PrimitiveOrWrapperParam(String name, String type, String fullTypeName, Class<?> clazz, AccessibleSchema accessibleSchema, JavaDtoSpec spec){
        this(name, new PrimitiveOrWrapperType(type, fullTypeName, clazz, spec), accessibleSchema);
    }

    public PrimitiveOrWrapperParam(String name, PrimitiveOrWrapperType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
        // primitive type is not nullable
        setNullable(getType().isWrapper);
    }

    public static PrimitiveOrWrapperParam build(String name, Class<?> clazz, AccessibleSchema accessibleSchema, JavaDtoSpec spec){
        if (clazz == Integer.class || clazz == int.class)
            return new IntParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema, spec);
        if (clazz == Boolean.class || clazz == boolean.class)
            return new BooleanParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema, spec);
        if (clazz == Double.class || clazz == double.class)
            return new DoubleParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema, spec);
        if (clazz == Float.class || clazz == float.class)
            return new FloatParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema, spec);
        if (clazz == Long.class || clazz == long.class)
            return new LongParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema, spec);
        if (clazz == Character.class || clazz == char.class)
            return new CharParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema, spec);
        if (clazz == Byte.class || clazz == byte.class)
            return new ByteParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema, spec);
        if (clazz == Short.class || clazz == short.class)
            return new ShortParam(name, clazz.getSimpleName(), clazz.getName(), clazz, accessibleSchema, spec);
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
        handleConstraintsInCopyDto(dto);
        return dto;
    }

    @Override
    public BigDecimal getMin() {
        return min;
    }

    @Override
    public void setMin(BigDecimal min) {
        if (this.min == null || (min != null && this.min.compareTo(min) < 0) )
            this.min = min;
    }

    @Override
    public BigDecimal getMax() {
        return max;
    }

    @Override
    public void setMax(BigDecimal max) {
        if (this.max == null || (max != null && this.max.compareTo(max) > 0) )
            this.max = max;
    }

    @Override
    public Object newInstance() {
        return getValue();
    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable) {
        String code;
        if (!getType().isWrapper && getValue() == null){
            // ignore instance of primitive types if the value is not assigned
            return Collections.emptyList();
        }
        if (accessibleSchema != null && accessibleSchema.setterMethodName != null){
            String castType = getCastType();
            if (castValueWithSpecificMethod(isJava)){
                castType = null;
            }
            code = CodeJavaOrKotlinGenerator.oneLineSetterInstance(accessibleSchema.setterMethodName, castType, variableName, castValueInTestGenerationIfNeeded(getValueAsJavaString(isJava), isJava),isJava, isVariableNullable);
        } else {
            if (accessibleSchema != null && !accessibleSchema.isAccessible)
                throw new IllegalStateException("Error: private field, but there is no setter method");
            String castType = getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
            if (castValueWithSpecificMethod(isJava)){
                castType = null;
            }

            code = CodeJavaOrKotlinGenerator.oneLineInstance(isDeclaration, doesIncludeName, castType, variableName, castValueInTestGenerationIfNeeded(getValueAsJavaString(isJava), isJava), isJava, isVariableNullable);
        }

        return Collections.singletonList(CodeJavaOrKotlinGenerator.getIndent(indent)+ code);
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        StringBuilder sb = new StringBuilder();
        sb.append(CodeJavaOrKotlinGenerator.getIndent(indent));
        if (getValue() == null)
            sb.append(CodeJavaOrKotlinGenerator.junitAssertNull(responseVarName, isJava));
        else
            sb.append(CodeJavaOrKotlinGenerator.junitAssertEquals(getValueAsJavaString(isJava), getPrimitiveValueInAssertion(responseVarName, isJava), isJava));

        return Collections.singletonList(sb.toString());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        setValueBasedOnStringValue(dto.stringValue);
    }

    public Object convertValueTo(Object value){
        Class type = getType().getClazz();
        String s = value.toString();
        if (Integer.class.equals(type) || int.class.equals(type)) {
            return Integer.valueOf(s);
        }else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return Boolean.valueOf(s);
        } else if (Double.class.equals(type) || double.class.equals(type)) {
            return Double.valueOf(s);
        } else if (Float.class.equals(type) ||  float.class.equals(type)) {
            return Float.valueOf(s);
        } else if (Long.class.equals(type) || long.class.equals(type)) {
            return Long.valueOf(value.toString());
        }  else if (Character.class.equals(type) || char.class.equals(type)) {
//            assert s.length() == 1;
            if (s.length() != 1)
                throw new IllegalArgumentException("it cannot be recognized as a char:"+s);
            return s.charAt(0);
        } else if (Byte.class.equals(type) || byte.class.equals(type)) {
            return Byte.valueOf(s);
        } else if (Short.class.equals(type) || short.class.equals(type)) {
            return Short.valueOf(s);
        }
        throw new RuntimeException("cannot find the type:"+type);
    }

    abstract public void setValueBasedOnStringValue(String stringValue);

    /**
     * @param responseVarName refers to the variable name in response
     * @param isJava
     * @return a string to get its primitive value if the param is Wrapper class
     * eg, res.byteValue() for byte with a response variable name res
     */
    public String getPrimitiveValueInAssertion(String responseVarName, boolean isJava) {
        boolean isWrapper = getType().isWrapper;
        if (accessibleSchema!=null && !accessibleSchema.isAccessible && accessibleSchema.getterReturn != null){
            isWrapper = !accessibleSchema.getterReturn.isPrimitive();
        }

        if (isWrapper)
            return methodInvocation(responseVarName, primitiveValueMethod(isJava), "", isJava, isNullable(), true);
        return responseVarName;
    }

    @Override
    public void copyProperties(NamedTypedValue copy) {
        super.copyProperties(copy);
        if (copy instanceof PrimitiveOrWrapperParam){
            ((PrimitiveOrWrapperParam)copy).setMin(min);
            ((PrimitiveOrWrapperParam)copy).setMax(max);
        }

        handleConstraintsInCopy(copy);
    }

    public boolean castValueWithSpecificMethod(boolean isJava){
        return false;
    }


    public String castValueInTestGenerationIfNeeded(String stringValue, boolean isJava){
        return getValueAsJavaString(isJava);
    }

    /**
     *
     * @return a cast type for this param, null means that there is no need to cast the value to a type
     */
    public String getCastType() {
        return null;
    }

    @Override
    public boolean getMinInclusive() {
        return this.minInclusive;
    }

    @Override
    public void setMinInclusive(boolean inclusive) {
        this.minInclusive = inclusive;
    }

    @Override
    public boolean getMaxInclusive() {
        return this.maxInclusive;
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

    @Override
    public List<String> referenceTypes() {
        return null;
    }



    abstract public String primitiveValueMethod(boolean isJava);
}
