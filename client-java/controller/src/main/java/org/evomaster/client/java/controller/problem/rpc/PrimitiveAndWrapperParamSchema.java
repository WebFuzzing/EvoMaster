package org.evomaster.client.java.controller.problem.rpc;

import java.util.Arrays;
import java.util.List;

/**
 * created by manzhang on 2021/11/3
 *
 */
public final class PrimitiveAndWrapperParamSchema extends ParamSchema{
    private final boolean isWrapper;
    public PrimitiveAndWrapperParamSchema(String type, String name, boolean isWrapper) {
        super(type, name);
        this.isWrapper = isWrapper;
    }

    public PrimitiveAndWrapperParamSchema(String type, String name){
        this(type, name, types.indexOf(type) >=8);
    }

    private final static List<String> types = Arrays.asList("int","byte","short","long","float","double","boolean","char","Integer","Byte","Short","Long","Float","Double","Boolean","Character");
    public static boolean isPrimitiveTypes(String type){
        return types.contains(type);
    }
}
