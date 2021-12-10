package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.MapType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * thrift
 *  HashMap (see https://thrift.apache.org/docs/types#containers)
 */
public class MapParam extends NamedTypedValue<MapType, List<PairParam>>{

    public MapParam(String name, MapType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;
        return getValue().stream().map(i-> {
            try {
                return new AbstractMap.SimpleEntry<>(i.getValue().getKey().newInstance(), i.getValue().getValue().newInstance());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(String.format("MapParam: could not create new instance for key and value (%s,%s)",
                        i.getValue().getKey().toString(), i.getValue().getValue().getType()));
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.MAP;
        if (getValue()!=null){
            dto.innerContent = getValue().stream().map(s->s.getDto()).collect(Collectors.toList());
        }
        return dto;
    }

    @Override
    public MapParam copyStructure() {
        return new MapParam(getName(), getType());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent!= null && !dto.innerContent.isEmpty()){
            PairParam t = getType().getTemplate();
            List<PairParam> values = dto.innerContent.stream().map(s-> {
                PairParam c = t.copyStructure();
                c.setValueBasedOnDto(s);
                return c;
            }).collect(Collectors.toList());
            setValue(values);
        }

    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        if (instance == null) return;
        PairParam t = getType().getTemplate();
        List<PairParam> values = new ArrayList<>();
        for (Object e : ((Map) instance).entrySet()){
            PairParam copy = t.copyStructure();
            copy.setValueBasedOnInstance(e);
            values.add(copy);
        }
        setValue(values);
    }
}
