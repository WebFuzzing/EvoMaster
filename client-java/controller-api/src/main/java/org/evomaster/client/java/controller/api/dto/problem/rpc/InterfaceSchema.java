package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, ParamSchema> typeCollections = new HashMap<>();


    public InterfaceSchema(String name, List<EndpointSchema> endpoints) {
        this.name = name;
        this.endpoints = endpoints;
    }

    public void registerType(ParamSchema param){
        String type = param.getType();
        if (!typeCollections.containsKey(type))
            typeCollections.put(type, param.copy());
    }

}
