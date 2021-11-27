package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.MapType;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
}
