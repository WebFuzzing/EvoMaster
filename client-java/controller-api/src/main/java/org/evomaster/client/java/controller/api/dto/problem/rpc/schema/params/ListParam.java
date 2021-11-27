package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.CollectionType;

import java.util.List;
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
}
