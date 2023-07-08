package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;

/**
 * type schema
 */
public abstract class TypeSchema {

    /**
     * simple name of the type
     */
    private final String type;
    /**
     * full name of the type, ie, including full package path
     */
    private final String fullTypeName;

    /**
     * original class
     */
    private final Class<?> clazz;

    public final JavaDtoSpec spec;

    private Class<?> originalType;

    /**
     * a depth of the type that are used by other types
     * eg, A contains B, and B contains C, then the depth for A is 2
     */
    public int depth;

    public TypeSchema(String type, String fullTypeName, Class<?> clazz, JavaDtoSpec spec){
        this.type = type;
        this.fullTypeName = fullTypeName;
        this.clazz = clazz;
        this.spec = spec;
    }

    public void setOriginalType(Class<?> originalType) {
        this.originalType = originalType;
    }

    public String getType() {
        return type;
    }

    public String getFullTypeName() {
        return fullTypeName;
    }

    public String getFullTypeNameWithGenericType(){
        return fullTypeName;
    }

    public abstract TypeSchema copy();

    public TypeDto getDto(){
        TypeDto dto = new TypeDto();
        dto.fullTypeName = fullTypeName;
        dto.fullTypeNameWithGenericType = getFullTypeNameWithGenericType();
        dto.depth = depth;
        return dto;
    }

    public boolean sameType(TypeDto dto){
        return fullTypeName.equals(dto.fullTypeName);
    }

    public Class<?> getClazz() {
        return originalType!= null? originalType: clazz;
    }

    public String getTypeNameForInstance(){
        return getFullTypeNameWithGenericType();
    }
}
