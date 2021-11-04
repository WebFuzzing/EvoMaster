package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * created by manzhang on 2021/11/3
 */
public final class CollectionParamSchema extends ParamSchema{
    private final String template;

    public CollectionParamSchema(String type, String name, String template) {
        super(type, type, name);
        this.template = template;
    }

    @Override
    public ParamSchema copy() {
        return new CollectionParamSchema(getType(), getName(), template);
    }

    public String getTemplate() {
        return template;
    }
}
