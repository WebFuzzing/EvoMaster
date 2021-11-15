package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

/**
 * created by manzhang on 2021/11/15
 */
public class TypeSchema {

    private final String type;
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
