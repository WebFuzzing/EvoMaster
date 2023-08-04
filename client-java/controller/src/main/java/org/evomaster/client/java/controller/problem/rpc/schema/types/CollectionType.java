package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

/**
 * collection type which includes Array, List, Set
 */
public class CollectionType extends TypeSchema{
    /**
     * template of elements of the collection
     */
    private final NamedTypedValue template;

    public CollectionType(String type, String fullTypeName, NamedTypedValue template, Class<?> clazz, JavaDtoSpec spec) {
        super(type, fullTypeName, clazz, spec);
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
    public String getTypeNameForInstanceInJavaOrKotlin(boolean isJava) {
        String generic = template.getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
        return CodeJavaOrKotlinGenerator.typeNameOfArrayOrCollection(getFullTypeName(), getClazz().isArray(), generic, isJava);
    }

    @Override
    public CollectionType copy() {
        return new CollectionType(getType(), getFullTypeName(), template, getClazz(), spec);
    }
}
