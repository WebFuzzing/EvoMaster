package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;


import java.util.ArrayList;

/**
 * cycle object
 */
public class CycleObjectType extends ObjectType{

    public CycleObjectType(String type, String fullTypeName) {
        super(type, fullTypeName, new ArrayList<>());
    }

}
