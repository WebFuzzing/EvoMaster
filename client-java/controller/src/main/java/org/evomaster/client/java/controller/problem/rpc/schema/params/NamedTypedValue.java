package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.TypeSchema;

import java.util.List;

/**
 * a named instance of the type with its value, eg Param/Field
 */
public abstract class NamedTypedValue<T extends TypeSchema, V> {

    public final static String NOT_NULL_MARK_OBJ_DATE = "{}";

    /**
     * name of the instance, eg param name
     */
    private final String name;

    /**
     * its type
     */
    private final T type;

    /**
     * its value
     */
    private V value;

    /**
     * represent whether this value is nullable
     */
    private boolean isNullable = true;

    /*
        TODO handle constraints
        ind1 uses javax-validation
     */

    public boolean isForAuth() {
        return isForAuth;
    }

    public void setForAuth(boolean forAuth) {
        isForAuth = forAuth;
    }

    private boolean isForAuth = false;

    public NamedTypedValue(String name, T type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public T getType() {
        return type;
    }

    public V getValue() {
        return value;
    }

    public abstract Object newInstance() throws ClassNotFoundException;

    public void setValue(V value) {
        this.value = value;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public boolean isNullable() {
        return isNullable;
    }

    /**
     * get its dto format in order to further handle it with evomastr core
     * @return its corresponding dto
     */
    public ParamDto getDto(){
        ParamDto dto = new ParamDto();
        dto.name = name;
        dto.type = type.getDto();
        dto.isNullable = isNullable;
        dto.isForAuth = isForAuth;
        return dto;
    }

    public abstract NamedTypedValue<T, V> copyStructure();


    /**
     * it is used to find a param schema based on info specified with dto format
     * @param dto specifies a param to check
     * @return whether [this] param schema info is consistent with the given dto
     */
    public boolean sameParam(ParamDto dto){
        return dto.name.equals(name) && type.sameType(dto.type);
    }

    /**
     * set value based on dto
     * the value is basically obtained from evomaster core
     * @param dto contains value info with string
     */
    public abstract void setValueBasedOnDto(ParamDto dto);

    /**
     * set value of param schema based on its instance
     * it is mainly used to parse response
     * @param instance a java object which is an instance of this param schema
     */
    public void setValueBasedOnInstance(Object instance){
        if (instance == null) return;
        if (isValidInstance(instance))
            setValueBasedOnValidInstance(instance);
        else
            throw new IllegalStateException("class of the instance ("+ instance.getClass().getName() +") does not conform with this param: "+getType().getFullTypeName());
    }

    protected abstract void setValueBasedOnValidInstance(Object instance);

    /**
     *
     * @param instance a java object which is an instance of this param schema
     * @return if the specified instance conforms with this param schema
     */
    public boolean isValidInstance(Object instance){
        return getType().getClazz().isInstance(instance);
    }


    /**
     * create instances with Java
     * @param isDeclaration specifies whether it is used to declare the instance (ie, with type name).
     * @param doesIncludeName specifies whether it is required to have the variable name
     *                        eg, if true, var = new instance(); if yes, new instance();
     * @param variableName specifies the name of variable
     * @param indent specifies the indent of this block of the code
     * @return a list of string which could create instance with java
     */
    public abstract List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent);


    /**
     * create instances with Java
     *
     * @param indent specifies the current indent of the code
     * @return a list of string which could create instance with java
     */
    public List<String> newInstanceWithJava(int indent){
        return newInstanceWithJava(true, true, getName(), indent);
    }

    /**
     * create assertions with java for response
     * @param indent specifies the current indent of the code
     * @param responseVarName a variable name for responses
     * @return a list of string for assertions
     */
    public abstract List<String> newAssertionWithJava(int indent, String responseVarName);

    public List<String> newAssertionWithJava(String responseVarName){
        return newAssertionWithJava(0, responseVarName);
    }

    public abstract String getValueAsJavaString();
}
