package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

/**
 * created by manzhang on 2021/11/15
 */
public class EnumType extends TypeSchema {

    private final String[] items;

    public EnumType(String type, String fullTypeName, String[] items) {
        super(type, fullTypeName);
        this.items = items;
    }

    public String[] getItems() {
        return items;
    }
}
