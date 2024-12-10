package org.evomaster.client.java.controller.problem.rpc.schema;

import org.evomaster.client.java.controller.DtoUtils;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCActionDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;

/**
 * endpoint dto for RPC service
 */
public class EndpointSchema {
    /**
     * name of the endpoint
     */
    private final String name;

    /**
     * name of the interface
     */
    private final String interfaceName;

    /**
     * name of type of the client
     */
    private final String clientTypeName;

    /**
     * request params of the endpoint
     */
    private final List<NamedTypedValue> requestParams;

    /**
     * response of the endpoint
     */
    private final NamedTypedValue response;

    /**
     * a list of exceptions which could throw from this endpoint
     */
    private final List<NamedTypedValue> exceptions;

    /**
     * whether the endpoint is clarified with auth
     */
    private final boolean authRequired;

    /**
     * a list of index of auth info based on what are configured in the driver
     */
    private final List<Integer> requiredAuthCandidates;

    public Set<String> getRelatedCustomizedCandidates() {
        return relatedCustomizedCandidates;
    }

    /**
     * a list of references of the related customizations related to this endpoint
     * the reference now is defined based on the index of a list specified in the driver
     */
    private final Set<String> relatedCustomizedCandidates;


    /**
     *
     * @param name is the name of the endpoint, ie, method name
     * @param interfaceName is the full name of the interface
     * @param clientTypeName is the client type with its full name
     * @param requestParams is a list of parameters in request
     * @param response is the response, ie, return
     * @param exceptions is related to exception for this endpoint
     * @param authRequired specifies whether the endpoint clearly declares that it requires auth, eg with annotation
     *                     note that if authRequired is false, the endpoint could also apply global auth setup
     * @param requiredAuthCandidates represents required candidates for its auth setup
     * @param relatedCustomizedCandidates represents related customizations based on their references
     */
    public EndpointSchema(String name, String interfaceName, String clientTypeName,
                          List<NamedTypedValue> requestParams, NamedTypedValue response, List<NamedTypedValue> exceptions,
                          boolean authRequired, List<Integer> requiredAuthCandidates, Set<String> relatedCustomizedCandidates) {
        this.name = name;
        this.interfaceName = interfaceName;
        this.clientTypeName = clientTypeName;
        this.requestParams = requestParams;
        this.response = response;
        this.exceptions = exceptions;
        this.authRequired = authRequired;
        this.requiredAuthCandidates = requiredAuthCandidates;
        this.relatedCustomizedCandidates = relatedCustomizedCandidates;
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

    public List<NamedTypedValue> getExceptions() {
        return exceptions;
    }

    /**
     *
     * @return a dto with a respect to this endpoint
     * such dto would be used between core and driver
     */
    public RPCActionDto getDto(){
        RPCActionDto dto = new RPCActionDto();
        dto.actionName = name;
        dto.interfaceId = interfaceName;
        dto.clientInfo = clientTypeName;
        if (requestParams != null)
            dto.requestParams = requestParams.stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        if (response != null)
            dto.responseParam = response.getDto();
        if (relatedCustomizedCandidates != null)
            dto.relatedCustomization = new HashSet<>(relatedCustomizedCandidates);
        if (requiredAuthCandidates != null)
            dto.requiredAuthCandidates = new ArrayList<>(requiredAuthCandidates);
        dto.isAuthorized = authRequired;
        return dto;
    }

    /**
     *
     * @param dto is a dto of rpc call which contains value info
     * @return if this endpoint matches the specified dto
     */
    public boolean sameEndpoint(RPCActionDto dto){
        return dto.actionName.equals(name)
                // only check input parameters
                // && (getResponse() == null || getResponse().sameParam(dto.responseParam))
                && ((getRequestParams() == null && dto.requestParams == null) || getRequestParams().size() == dto.requestParams.size())
                && IntStream.range(0, getRequestParams().size()).allMatch(i-> getRequestParams().get(i).sameParam(dto.requestParams.get(i)));
    }

    /**
     * find an endpoint schema based on seeded tests
     * @param dto a seeded test dto
     * @return an endpoint schema
     */
    public boolean sameEndpoint(SeededRPCActionDto dto){
        return dto.functionName.equals(name)
                // only check input parameters
                // && (getResponse() == null || getResponse().sameParam(dto.responseParam))
                && ((getRequestParams() == null && dto.inputParamTypes == null) || getRequestParams().size() == dto.inputParamTypes.size())
                && IntStream.range(0, getRequestParams().size()).allMatch(i-> getRequestParams().get(i).getType().getFullTypeName().equals(dto.inputParamTypes.get(i)));
    }

    /**
     *
     * @return a copy of this endpoint which contains its structure but not values
     */
    public EndpointSchema copyStructure(){
        return new EndpointSchema(
                name, interfaceName, clientTypeName,
                requestParams == null? null: requestParams.stream().map(NamedTypedValue::copyStructureWithProperties).collect(Collectors.toList()),
                response == null? null: response.copyStructureWithProperties(), exceptions == null? null: exceptions.stream().map(NamedTypedValue::copyStructureWithProperties).collect(Collectors.toList()),
                authRequired, requiredAuthCandidates, relatedCustomizedCandidates);
    }

    /**
     * set value of endpoint based on dto
     * @param dto contains value info the endpoint
     *            note that the dto is typically generated by core side, ie, search
     */
    public void setValue(RPCActionDto dto){
        if (dto.requestParams != null ){
            IntStream.range(0, dto.requestParams.size()).forEach(s-> requestParams.get(s).setValueBasedOnDto(dto.requestParams.get(s)));
        }
        // might be not useful
        if (dto.responseParam != null)
            response.setValueBasedOnDto(dto.responseParam);
    }

    /**
     * process to generate java code to invoke this request
     *
     * @param responseVarName specifies a variable name representing a response of this endpoint
     * @param clientVariable
     * @param outputFormat
     * @return code to send the request and set the response if exists
     */
    public List<String> newInvocationWithJavaOrKotlin(String responseVarName, String controllerVarName, String clientVariable, SutInfoDto.OutputFormat outputFormat){
        List<String> javaCode = new ArrayList<>();
        if (response != null){
            boolean isPrimitive = (response.getType() instanceof PrimitiveOrWrapperType) && !((PrimitiveOrWrapperType)response.getType()).isWrapper;
            javaCode.add(oneLineInstance(true, true, response.getType().getTypeNameForInstanceInJavaOrKotlin(DtoUtils.isJava(outputFormat)), responseVarName, null, isPrimitive, DtoUtils.isJava(outputFormat), response.isNullable()));
        }
        javaCode.add(codeBlockStart(DtoUtils.isJava(outputFormat)));
        int indent = 1;
        for (NamedTypedValue param: getRequestParams()){
            javaCode.addAll(param.newInstanceWithJavaOrKotlin(indent, DtoUtils.isJava(outputFormat), param.isNullable()));
        }
        String paramVars = requestParams.stream().map(NamedTypedValue::getName).collect(Collectors.joining(","));
        String client = clientVariable;

        if (client == null)
            client = castToType(clientTypeName, getGetClientMethod(controllerVarName,"\""+handleEscapeCharInString(interfaceName, DtoUtils.isJava(outputFormat))+"\""), DtoUtils.isJava(outputFormat) );

        if (client == null){
            throw new IllegalArgumentException("fail to generate code for accessing client :"+clientTypeName);
        }

        addCode(
                javaCode,
                setInstance(response!= null,
                        responseVarName,
                        methodInvocation(client, getName(), paramVars, DtoUtils.isJava(outputFormat), (response!= null) ? response.isNullable() : true, false), DtoUtils.isJava(outputFormat)),
                indent);

        javaCode.add(codeBlockEnd(DtoUtils.isJava(outputFormat)));
        return javaCode;
    }
}
