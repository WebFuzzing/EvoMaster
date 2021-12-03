package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.MapType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * thrift
 *  HashMap (see https://thrift.apache.org/docs/types#containers)
 */
public class MapParam extends NamedTypedValue<MapType, Map<NamedTypedValue, NamedTypedValue>>{

    public MapParam(String name, MapType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return getValue().entrySet().stream().map(i-> {
            try {
                return new AbstractMap.SimpleEntry<>(i.getValue().newInstance(), i.getValue().newInstance());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(String.format("MapParam: could not create new instance for key and value (%s,%s)", i.getKey().toString(), i.getValue().getType()));
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.MAP;
        dto.type.example = getType().getTemplate().getDto();
        return dto;
    }

    @Override
    public MapParam copyStructure() {
        return new MapParam(getName(), getType());
    }

    @Override
    public void setValue(ParamDto dto) {
        if (!dto.innerContent.isEmpty()){
            PairParam t = getType().getTemplate();
            Map<NamedTypedValue, NamedTypedValue> values = dto.innerContent.stream().map(s-> {
                PairParam c = t.copyStructure();
                c.setValue(s);
                return c.getValue();
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            setValue(values);
        }

    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        PairParam t = getType().getTemplate();
        Map<NamedTypedValue, NamedTypedValue> values = new HashMap<>();
        for (Object e : ((Map) instance).entrySet()){
            PairParam copy = t.copyStructure();
            copy.setValueBasedOnInstance(e);
            values.put(copy.getValue().getKey(), copy.getValue().getValue());
        }
        setValue(values);
    }
}
