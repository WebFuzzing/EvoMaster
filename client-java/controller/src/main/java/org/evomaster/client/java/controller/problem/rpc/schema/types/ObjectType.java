package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

import java.util.List;

/**
 * object type
 */
public class ObjectType extends TypeSchema {
    /**
     * a list of fields of the object
     */
    private final List<NamedTypedValue> fields;

    public ObjectType(String type, String fullTypeName, List<NamedTypedValue> fields, Class<?> clazz) {
        super(type, fullTypeName, clazz);
        this.fields = fields;
    }

    public List<NamedTypedValue> getFields() {
        return fields;
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.CUSTOM_OBJECT;
        return dto;
    }
}
