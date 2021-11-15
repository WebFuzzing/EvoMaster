package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import java.util.List;

/**
 * created by manzhang on 2021/11/15
 */
public class ObjectType extends TypeSchema {
    private final List<String> names;
    private final List<TypeSchema> types;


    public ObjectType(String type, String fullTypeName, List<String> names, List<TypeSchema> types) {
        super(type, fullTypeName);
        this.names = names;
        this.types = types;
    }
}
