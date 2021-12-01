package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.StringType;

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
        setValue(dto.jsonValue);
    }

}
