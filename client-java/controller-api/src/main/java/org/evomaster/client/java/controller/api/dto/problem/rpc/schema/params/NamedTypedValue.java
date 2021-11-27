package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.TypeSchema;

import java.io.Serializable;

/**
 * a named instance of the type with its value, eg Param/Field
 */
public abstract class NamedTypedValue<T extends TypeSchema, V> {

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
    private boolean isNullable;

    /*
        TODO handle constraints
        ind1 uses javax-validation
     */

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

    public ParamDto getDto(){
        ParamDto dto = new ParamDto();
        dto.name = name;
        dto.type = type.getDto();
        return dto;
    }

    public abstract NamedTypedValue<T, V> copyStructure();
}
