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

    /**
     * input params for setter
     */
    public final Class<?>[] setterInputParams;

    public AccessibleSchema(){
        this(true, null, null, null);
    }

    public AccessibleSchema(boolean isAccessible, String setterMethodName, String getterMethodName){
        this(isAccessible, setterMethodName, getterMethodName, null);
    }
    /**
     *
     * @param isAccessible specifies if the field is public
     * @param setterMethodName specifies the setter method name if it exists
     * @param getterMethodName specifies the getter method name if it exists
     * @param setterInputParams specifies types of input params
     */
    public AccessibleSchema(boolean isAccessible, String setterMethodName, String getterMethodName, Class<?>[] setterInputParams){
        this.getterMethodName = getterMethodName;
        this.isAccessible = isAccessible;
        this.setterMethodName = setterMethodName;
        this.setterInputParams = setterInputParams;
    }
}
