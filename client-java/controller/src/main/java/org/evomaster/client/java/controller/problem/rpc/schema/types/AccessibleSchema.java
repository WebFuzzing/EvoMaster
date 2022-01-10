package org.evomaster.client.java.controller.problem.rpc.schema.types;

public class AccessibleSchema {

    public final boolean isAccessible;

    public final String setterMethodName;

    public final String getterMethodName;

    public AccessibleSchema(){
        this(true, null, null);
    }

    public AccessibleSchema(boolean isAccessible, String setterMethodName, String getterMethodName){
        this.getterMethodName = getterMethodName;
        this.isAccessible = isAccessible;
        this.setterMethodName = setterMethodName;
    }
}
