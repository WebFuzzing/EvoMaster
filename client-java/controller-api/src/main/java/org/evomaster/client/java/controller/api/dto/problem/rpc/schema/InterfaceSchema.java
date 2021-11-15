package org.evomaster.client.java.controller.api.dto.problem.rpc.schema;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.TypeSchema;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * created by manzhang on 2021/11/3
 */
public final class InterfaceSchema implements Serializable {
    private final String name;
    private List<EndpointSchema> endpoints;

    /**
     * key is the name of type
     * value is one example
     */
    private Map<String, TypeSchema> typeCollections = new HashMap<>();


    public InterfaceSchema(String name, List<EndpointSchema> endpoints) {
        this.name = name;
        this.endpoints = endpoints;
    }

    public void registerType(TypeSchema type){
        String typeName = type.getFullTypeName();
        if (!typeCollections.containsKey(typeName))
            typeCollections.put(typeName, type.copy());
    }

    public TypeSchema getTypeOrNull(String name){
        return typeCollections.get(name);
    }

    public List<EndpointSchema> getEndpoints(){
        return endpoints;
    }

    public List<EndpointSchema> findEndpoints(String name){
        return endpoints.stream().filter(s-> s.getName().equals(name)).collect(Collectors.toList());
    }

}
