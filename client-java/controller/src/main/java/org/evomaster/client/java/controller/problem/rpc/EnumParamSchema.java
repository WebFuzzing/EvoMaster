package org.evomaster.client.java.controller.problem.rpc;

import java.util.List;

/**
 * created by manzhang on 2021/11/3
 */
public class EnumParamSchema extends ParamSchema{
    private final List<String> items;

    public EnumParamSchema(String type, String name, List<String> items) {
        super(type, name);
        this.items = items;
    }
}
