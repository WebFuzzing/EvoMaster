package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CollectionType;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * thrift
 *     HashSet (see https://thrift.apache.org/docs/types#containers)
 */
public class SetParam extends NamedTypedValue<CollectionType, Set<NamedTypedValue>>{

    public SetParam(String name, CollectionType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;
        return getValue().stream().map(v-> {
            try {
                return v.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("ArrayParam: could not create new instance for value:"+v.getType());
            }
        }).collect(Collectors.toSet());
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.SET;
        if (getValue() != null){
            dto.innerContent = getValue().stream().map(s-> s.getDto()).collect(Collectors.toList());
        }
        return dto;
    }

    @Override
    public SetParam copyStructure() {
        return new SetParam(getName(), getType());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent!= null && !dto.innerContent.isEmpty()){
            NamedTypedValue t = getType().getTemplate();
            Set<NamedTypedValue> values = dto.innerContent.stream().map(s-> {
                NamedTypedValue v = t.copyStructure();
                v.setValueBasedOnDto(s);
                return v;
            }).collect(Collectors.toSet());
            setValue(values);
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        NamedTypedValue t = getType().getTemplate();
        Set<NamedTypedValue> values = new HashSet<>();
        for (Object e : (Set) instance){
            NamedTypedValue copy = t.copyStructure();
            copy.setValueBasedOnInstance(e);
            values.add(copy);
        }
        setValue(values);
    }
}
