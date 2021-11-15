package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.CollectionType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.TypeSchema;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * thrift -> HashSet (see https://thrift.apache.org/docs/types#containers)
 */
public class SetParam extends NamedTypedValue<CollectionType, Set>{
    Set<NamedTypedValue> values;

    public SetParam(String name, CollectionType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return values.stream().map(v-> {
            try {
                return v.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("ArrayParam: could not create new instance for value:"+v.getType());
            }
        }).collect(Collectors.toSet());
    }
}
