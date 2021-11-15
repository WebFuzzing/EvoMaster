package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

/**
 * created by manzhang on 2021/11/15
 */
public class CollectionType extends TypeSchema{
    private final NamedTypedValue template;

    public CollectionType(String type, String fullTypeName, NamedTypedValue template) {
        super(type, fullTypeName);
        this.template = template;
    }

}
