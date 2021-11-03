package org.evomaster.client.java.controller.problem.rpc;

import java.io.Serializable;
import java.util.List;

/**
 * created by manzhang on 2021/11/3
 */
public final class InterfaceSchema implements Serializable {
    private final String name;
    private List<EndpointSchema> endpoints;


    public InterfaceSchema(String name, List<EndpointSchema> endpoints) {
        this.name = name;
        this.endpoints = endpoints;
    }
}
