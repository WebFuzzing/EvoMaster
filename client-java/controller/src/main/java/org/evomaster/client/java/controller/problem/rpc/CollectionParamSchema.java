package org.evomaster.client.java.controller.problem.rpc;

/**
 * created by manzhang on 2021/11/3
 */
public final class CollectionParamSchema extends ParamSchema{
    private final String template;

    public CollectionParamSchema(String type, String name, String template) {
        super(type, name);
        this.template = template;
    }
}
