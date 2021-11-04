package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * created by manzhang on 2021/11/3
 */
public final class StringParamSchema extends ParamSchema{

    public final static String STRING_TYPE_NAME = "String";

    public StringParamSchema(String name) {
        super(STRING_TYPE_NAME, STRING_TYPE_NAME, name);
    }

    @Override
    public ParamSchema copy() {
        return new StringParamSchema(getName());
    }
}
