package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

/**
 * collection type which includes Array, List, Set
 */
public class CollectionType extends TypeSchema{
    /**
     * template of elements of the collection
     */
    private final NamedTypedValue template;

    public CollectionType(String type, String fullTypeName, NamedTypedValue template, Class<?> clazz) {
        super(type, fullTypeName, clazz);
        this.template = template;
    }

    public NamedTypedValue getTemplate() {
        return template;
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.example = template.getDto();
        return dto;
    }

    @Override
    public String getTypeNameForInstance() {
        String generic = template.getType().getTypeNameForInstance();
        if (getClazz().isArray())
            return generic+"[]";
        return getFullTypeName()+"<"+generic+">";
    }

    @Override
    public CollectionType copyContent() {
        return new CollectionType(getType(), getFullTypeName(), template, getClazz());
    }
}
