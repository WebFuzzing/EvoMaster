package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

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
public class MapParam extends NamedTypedValue<MapType, Map>{
    public List<NamedTypedValue> keys;
    public List<NamedTypedValue> values;

    public MapParam(String name, MapType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (keys.size() != values.size())
            throw new IllegalStateException(String.format("mismatched size of keys (%d) and values (%d)", keys.size(), values.size()));

        return IntStream.range(0, keys.size()).mapToObj(i-> {
                    try {
                        return new AbstractMap.SimpleEntry<>(keys.get(i).newInstance(), values.get(i).newInstance());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(String.format("MapParam: could not create new instance for key and value (%s,%s)", keys.get(i).getValue().toString(), values.get(i).getType()));
                    }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
