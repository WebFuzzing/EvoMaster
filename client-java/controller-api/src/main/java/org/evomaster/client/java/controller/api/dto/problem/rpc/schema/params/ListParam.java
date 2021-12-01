package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.CollectionType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * thrift
 *  ArrayList (see https://thrift.apache.org/docs/types#containers)
 */
public class ListParam extends NamedTypedValue<CollectionType, List<NamedTypedValue>>{

    public ListParam(String name, CollectionType type) {
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
        }).collect(Collectors.toList());
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.LIST;
        dto.type.example = getType().getTemplate().getDto();
        return dto;
    }

    @Override
    public ListParam copyStructure() {
        return new ListParam(getName(), getType());
    }

    @Override
    public void setValue(ParamDto dto) {
        if (!dto.innerContent.isEmpty()){
            NamedTypedValue t = getType().getTemplate();
            List<NamedTypedValue> values = dto.innerContent.stream().map(s-> {
                NamedTypedValue v = t.copyStructure();
                t.setValue(s);
                return v;
            }).collect(Collectors.toList());
            setValue(values);
        }
    }


}
