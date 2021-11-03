package org.evomaster.client.java.controller.problem.rpc;

import java.util.List;

/**
 * created by manzhang on 2021/11/3
 */
public class ObjectParamSchema extends ParamSchema{
    private final List<ParamSchema> fields;

    public ObjectParamSchema(String type, String name, List<ParamSchema> fields) {
        super(type, name);
        this.fields = fields;
    }
}
