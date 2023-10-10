package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;

/**
 * enumeration
 */
public class EnumType extends TypeSchema {

    /**
     * items in this enumeration
     * here we only collect name of the items
     */
    private final String[] items;

    public EnumType(String type, String fullTypeName, String[] items, Class<?> clazz, JavaDtoSpec spec) {
        super(type, fullTypeName, clazz, spec);
        this.items = items;
    }

    public String[] getItems() {
        return items;
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.ENUM;
        dto.fixedItems = items;
        return dto;
    }

    @Override
    public EnumType copy() {
        return new EnumType(getSimpleTypeName(), getFullTypeName(), items, getClazz(), spec);
    }
}
