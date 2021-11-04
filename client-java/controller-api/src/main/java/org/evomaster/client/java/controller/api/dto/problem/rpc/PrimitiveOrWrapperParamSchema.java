package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.Arrays;
import java.util.List;

/**
 * created by manzhang on 2021/11/3
 *
 */
public final class PrimitiveOrWrapperParamSchema extends ParamSchema{
    private final boolean isWrapper;
    public PrimitiveOrWrapperParamSchema(String type, String name, boolean isWrapper) {
        super(type, type, name);
        if (!isPrimitiveOrTypes(type))
            throw new IllegalStateException("the type is not Primitive Or Wrapper class: "+ type);
        this.isWrapper = isWrapper;
    }

    public PrimitiveOrWrapperParamSchema(String type, String name){
        this(type, name, types.indexOf(type) >=8);
    }

    @Override
    public ParamSchema copy() {
        return new PrimitiveOrWrapperParamSchema(getType(), getName(), isWrapper);
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
}
