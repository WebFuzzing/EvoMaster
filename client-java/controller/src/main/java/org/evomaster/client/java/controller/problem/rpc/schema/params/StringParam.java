package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.StringType;

/**
 * string param
 */
public class StringParam extends NamedTypedValue<StringType, String> {

    public StringParam(String name) {
        super(name, new StringType());
    }

    @Override
    public Object newInstance() {
        return getValue();
    }

    @Override
    public StringParam copyStructure() {
        return new StringParam(getName());
    }

    @Override
    public void setValue(ParamDto dto) {
        if (dto.jsonValue != null)
            setValue(dto.jsonValue);
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((String) instance);
    }

}
