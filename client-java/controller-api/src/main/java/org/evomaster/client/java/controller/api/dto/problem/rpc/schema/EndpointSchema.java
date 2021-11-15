package org.evomaster.client.java.controller.api.dto.problem.rpc.schema;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

import java.io.Serializable;
import java.util.List;

/**
 * created by manzhang on 2021/11/3
 */
public final class EndpointSchema implements Serializable {
    private final String name;
    private final List<NamedTypedValue> requestParams;
    private final NamedTypedValue response;

    //TODO handle throw exception of the method

    public EndpointSchema(String name, List<NamedTypedValue> requestParams, NamedTypedValue response) {
        this.name = name;
        this.requestParams = requestParams;
        this.response = response;
    }

    public String getName() {
        return name;
    }

    public List<NamedTypedValue> getRequestParams() {
        return requestParams;
    }

    public NamedTypedValue getResponse() {
        return response;
    }
}
