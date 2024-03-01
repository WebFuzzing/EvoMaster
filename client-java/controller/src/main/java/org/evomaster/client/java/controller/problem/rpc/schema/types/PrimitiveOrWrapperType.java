package org.evomaster.client.java.controller.problem.rpc.schema.types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * primitive types
 */
public class PrimitiveOrWrapperType extends TypeSchema {

    /**
     * represent if the type is wrapper
     * for instance, isWrapper for Integer is true and for int is false
     */
    public final boolean isWrapper;

    private final static List<Class<?>> INTEGRAL_NUMBER = Arrays.asList(Byte.class, byte.class, Short.class, short.class, Integer.class, int.class, Long.class, long.class);
    private final static List<Class<?>> FLOATINGPOINT_NUMBER = Arrays.asList(Float.class, float.class, Double.class, double.class);


    private final static Map<Class, String> javaToKotlin = new HashMap<Class, String>(){{
        put(Byte.class, "Byte");
        put(byte.class, "Byte");

        put(Short.class, "Short");
        put(short.class, "Short");

        put(Integer.class, "Int");
        put(int.class, "Int");

        put(Long.class, "Long");
        put(long.class, "Long");

        put(Float.class, "Float");
        put(float.class, "Float");

        put(Double.class, "Double");
        put(double.class, "Double");

        put(Boolean.class, "Boolean");
        put(boolean.class, "Boolean");

        put(Character.class, "Char");
        put(char.class, "Char");
    }};

    public PrimitiveOrWrapperType(String type, String fullTypeName, boolean isWrapper, Class<?> clazz, JavaDtoSpec spec) {
        super(type, fullTypeName, clazz, spec);
        if (!isPrimitiveOrTypes(type))
            throw new IllegalStateException("the type is not Primitive Or Wrapper class: "+ type);
        this.isWrapper = isWrapper;
    }

    public PrimitiveOrWrapperType(String type, String fullTypeName, Class<?> clazz, JavaDtoSpec spec){
        this(type, fullTypeName, types.indexOf(type) >=8, clazz, spec);
    }

    private final static List<String> types = Arrays.asList("int","byte","short","long","float","double","boolean","char","Integer","Byte","Short","Long","Float","Double","Boolean","Character");

    public static boolean isPrimitiveOrTypes(String type){
        return types.contains(type);
    }

    public static boolean isPrimitiveOrTypes(Class<?> clazz){
        if (clazz.isPrimitive()) return true;
        return  clazz == Integer.class || clazz == Byte.class || clazz == Short.class || clazz == Long.class ||
                clazz== Float.class || clazz == Double.class || clazz == Boolean.class || clazz == Character.class;
    }


    @Override
    public PrimitiveOrWrapperType copy() {
        return new PrimitiveOrWrapperType(getSimpleTypeName(), getFullTypeName(), isWrapper, getClazz(), spec);
    }

    public boolean isNumber(){
        return isIntegralNumber() || isFloatingPointNumber();
    }

    public boolean isFloatingPointNumber(){
        return FLOATINGPOINT_NUMBER.contains(getClazz());
    }

    public boolean isIntegralNumber(){
        return INTEGRAL_NUMBER.contains(getClazz());
    }

    @Override
    public String getTypeNameForInstanceInJavaOrKotlin(boolean isJava) {
        if (!isJava)
            return javaToKotlin.get(getClazz());
        return getSimpleTypeName();
    }
}
