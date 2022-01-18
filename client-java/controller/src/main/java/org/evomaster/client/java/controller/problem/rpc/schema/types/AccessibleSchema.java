package org.evomaster.client.java.controller.problem.rpc.schema.types;

/**
 * info about if the field is public
 * and if not, how to get/set value
 */
public class AccessibleSchema {

    /**
     * is public?
     */
    public final boolean isAccessible;

    /**
     * setter method name if it is not public
     */
    public final String setterMethodName;

    /**
     * getter method name if it is not public
     */
    public final String getterMethodName;

    public AccessibleSchema(){
        this(true, null, null);
    }

    /**
     *
     * @param isAccessible specifies if the field is public
     * @param setterMethodName specifies the setter method name if it exists
     * @param getterMethodName specifies the getter method name if it exists
     */
    public AccessibleSchema(boolean isAccessible, String setterMethodName, String getterMethodName){
        this.getterMethodName = getterMethodName;
        this.isAccessible = isAccessible;
        this.setterMethodName = setterMethodName;
    }
}
