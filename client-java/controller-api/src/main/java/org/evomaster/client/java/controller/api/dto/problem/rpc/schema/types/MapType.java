package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

/**
 * created by manzhang on 2021/11/15
 */
public class MapType extends TypeSchema{
    private final NamedTypedValue keyTemplate;
    private final NamedTypedValue valueTemplate;

    public MapType(String type, String fullTypeName, NamedTypedValue keyTemplate, NamedTypedValue valueTemplate) {
        super(type, fullTypeName);
        this.keyTemplate = keyTemplate;
        this.valueTemplate = valueTemplate;
    }
}
