package org.evomaster.client.java.controller.api.dto.problem.rpc.schema;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

import java.io.Serializable;
import java.util.List;

/**
 * endpoint dto for RPC service
 */
public final class EndpointSchema implements Serializable {
    /**
     * name of the endpoint
     */
    private final String name;

    /**
     * request params of the endpoint
     */
    private final List<NamedTypedValue> requestParams;

    /**
     * response of the endpoint
     */
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
