package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

import java.util.List;
import java.util.Set;

/**
 * collection type which includes Array, List, Set
 */
public class CollectionType extends TypeSchema{

    private final static String KOTLIN_LIST = "MutableList";
    private final static String KOTLIN_SET = "MutableSet";

    private final static String KOTLIN_ARRAY = "Array";

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
        String collectionType = getFullTypeName();
        if (!isJava){
            // adapt to kolin
            if (List.class.isAssignableFrom(getClazz())){
                collectionType = KOTLIN_LIST;
            }else if (Set.class.isAssignableFrom(getClazz())){
                collectionType = KOTLIN_SET;
            }else if (getClazz().isArray()){
                collectionType = KOTLIN_ARRAY;
            }
        }

        return CodeJavaOrKotlinGenerator.typeNameOfArrayOrCollection(collectionType, getClazz().isArray(), generic, isJava);
    }

    @Override
    public CollectionType copy() {
        return new CollectionType(getSimpleTypeName(), getFullTypeName(), template, getClazz(), spec);
    }

    @Override
    public String getFullTypeNameWithGenericType() {
        String generic = template.getType().getFullTypeNameWithGenericType();
        if (getClazz().isArray())
            return super.getFullTypeNameWithGenericType();
        return getFullTypeName()+"<"+generic+">";
    }
}
