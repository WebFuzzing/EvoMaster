package org.evomaster.client.java.controller.problem.rpc.schema;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * endpoint dto for RPC service
 */
public final class EndpointSchema {
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

    public RPCActionDto getDto(){
        RPCActionDto dto = new RPCActionDto();
        dto.actionId = name;
        dto.requestParams = requestParams.stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        if (response != null)
            dto.responseParam = response.getDto();
        return dto;
    }

    public boolean sameEndpoint(RPCActionDto dto){
        return dto.actionId.equals(name)
                && (getResponse() == null || getResponse().sameParam(dto.responseParam))
                && getRequestParams().size() == dto.requestParams.size()
                && IntStream.range(0, getRequestParams().size()).allMatch(i-> getRequestParams().get(i).sameParam(dto.requestParams.get(i)));
    }

    public EndpointSchema copyStructure(){
        return new EndpointSchema(
                name,
                requestParams.stream().map(NamedTypedValue::copyStructure).collect(Collectors.toList()),
                response == null? null: response.copyStructure()
        );
    }

    public void setValue(RPCActionDto dto){
        if (dto.requestParams != null){
            IntStream.range(0, dto.requestParams.size()).forEach(s-> requestParams.get(s).setValue(dto.requestParams.get(s)));
        }
        if (dto.responseParam != null)
            response.setValue(dto.responseParam);
    }
}
