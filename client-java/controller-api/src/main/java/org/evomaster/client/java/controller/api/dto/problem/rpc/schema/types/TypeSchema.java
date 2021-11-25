package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

/**
 * type schema
 */
public class TypeSchema {

    /**
     * simple name of the type
     */
    private final String type;
    /**
     * full name of the type, ie, including full package path
     */
    private final String fullTypeName;

    public TypeSchema(String type, String fullTypeName){
        this.type = type;
        this.fullTypeName = fullTypeName;
    }


    public String getType() {
        return type;
    }

    public String getFullTypeName() {
        return fullTypeName;
    }

    public TypeSchema copy(){
        return new TypeSchema(type, fullTypeName);
    }
}
