package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

/**
 * enumeration
 */
public class EnumType extends TypeSchema {

    /**
     * items in this enumeration
     * here we only collect name of the items
     */
    private final String[] items;

    public EnumType(String type, String fullTypeName, String[] items) {
        super(type, fullTypeName);
        this.items = items;
    }

    public String[] getItems() {
        return items;
    }
}
