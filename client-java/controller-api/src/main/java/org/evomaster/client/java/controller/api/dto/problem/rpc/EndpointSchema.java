package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.io.Serializable;
import java.util.List;

/**
 * created by manzhang on 2021/11/3
 */
public final class EndpointSchema implements Serializable {
    private final List<ParamSchema> requestParams;
    private final ParamSchema response;

    public EndpointSchema(List<ParamSchema> requestParams, ParamSchema response) {
        this.requestParams = requestParams;
        this.response = response;
    }
}
