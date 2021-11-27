package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.CollectionType;

import java.lang.reflect.Array;
import java.util.List;

/**
 * array param
 */
public class ArrayParam extends NamedTypedValue<CollectionType, List<NamedTypedValue>>{

    public ArrayParam(String name, CollectionType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return getValue().stream().map(v-> {
            try {
                return v.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("ArrayParam: could not create new instance for value:"+v.getType());
            }
        }).toArray();
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.ARRAY;
        dto.type.example = getType().getTemplate().getDto();
        return dto;
    }

    @Override
    public ArrayParam copyStructure() {
        return new ArrayParam(getName(), getType());
    }


}
