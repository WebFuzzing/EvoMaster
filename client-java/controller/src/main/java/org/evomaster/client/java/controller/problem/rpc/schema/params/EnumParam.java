package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.EnumType;

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

    @Override
    public void setValue(ParamDto dto) {
        try {
            setValue(Integer.parseInt(dto.jsonValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+dto.jsonValue+" as int value for setting enum");
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        //TODO
    }
}
