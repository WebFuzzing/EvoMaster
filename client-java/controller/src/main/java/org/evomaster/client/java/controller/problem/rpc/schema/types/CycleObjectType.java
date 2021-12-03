package org.evomaster.client.java.controller.problem.rpc.schema.types;


import java.util.ArrayList;

/**
 * cycle object
 */
public class CycleObjectType extends ObjectType{

    public CycleObjectType(String type, String fullTypeName, Class<?> clazz) {
        super(type, fullTypeName, new ArrayList<>(), clazz);
    }

}
