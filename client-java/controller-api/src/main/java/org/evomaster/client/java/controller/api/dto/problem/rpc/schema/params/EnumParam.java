package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.EnumType;

/**
 * enum parameter
 */
public class EnumParam extends NamedTypedValue<EnumType, Integer> {


    public EnumParam(String name, EnumType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        Class <? extends Enum> clazz = (Class < ? extends Enum >) Class.forName(getType().getFullTypeName());
        String value = getType().getItems()[getValue()];
        return Enum.valueOf(clazz, value);
    }

    @Override
    public EnumParam copyStructure() {
        return new EnumParam(getName(), getType());
    }
}
