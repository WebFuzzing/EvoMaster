package org.evomaster.client.java.controller.problem.rpc.schema;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCInterfaceSchemaDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCActionDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CycleObjectType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.TypeSchema;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * schema dto of the RCP service
 */
public final class InterfaceSchema{
    /**
     * name of the interface
     */
    private final String name;

    /**
     * name of the client
     */
    private final String clientInfo;

    /**
     * a list of available endpoints in the service
     */
    private List<EndpointSchema> endpoints;

    public Map<Integer, EndpointSchema> getAuthEndpoints() {
        return authEndpoints;
    }

    /**
     * a map of endpoints with their references for handling authentication
     * key - index of the auth info specified in the driver
     * value - the endpoint for handling such authentication with concrete info
     *
     * note that compared with [endpointsForAuth], authEndpoints contain concrete info for invocation
     * eg, for a login endpoint, it might have different inputs representing different authentication
     */
    private Map<Integer, EndpointSchema> authEndpoints;

    /**
     * a list of endpoints (in this interface) which are responsible for auth setup
     * eg, login
     */
    private List<EndpointSchema> endpointsForAuth;

    /**
     * key is the full name of type
     * value is its type schema
     */
    private Map<String, TypeSchema> typeCollections = new HashMap<>();

    /**
     * key is the full name of type
     * value is one example of param with the TypeSchema
     */
    private Map<String, NamedTypedValue> objParamCollections = new HashMap<>();

    /**
     * type of the RPC
     */
    private final RPCType rpcType;

    /**
     * a list of endpoints which are skipped to test
     */
    private final List<String> skippedEndpoints;

    /**
     * key is the full name of type
     */
    private Map<String, NamedTypedValue> identifiedResponseTypes = new HashMap<>();

    /**
     *
     * @param name is the name of the interface
     * @param endpoints is a list of endpoints which are involved for testing
     * @param client is the client name
     * @param rpcType is the rpc type
     */
    public InterfaceSchema(String name, List<EndpointSchema> endpoints, String client, RPCType rpcType) {
        this(name, endpoints, client, rpcType, null, null, null);
    }

    /**
     *
     * @param name is the name of the interface
     * @param endpoints is a list of endpoints which are involved for testing
     * @param client is the client name
     * @param rpcType is the rpc type
     * @param skippedEndpoints is a list of endpoints which are specified to be skipped
     * @param authEndpoints is a map of authentication info which could be handled with the endpoint
     *                      key - index of authentication info in the driver
     *                      value - the endpoint which contains concrete info for its invocation
     * @param endpointsForAuth is a list of endpoints in this interface that are responsible for auth setup
     */
    public InterfaceSchema(String name, List<EndpointSchema> endpoints, String client, RPCType rpcType, List<String> skippedEndpoints, Map<Integer, EndpointSchema> authEndpoints, List<EndpointSchema> endpointsForAuth) {
        this.name = name;
        this.endpoints = endpoints;
        this.clientInfo = client;
        this.rpcType = rpcType;
        this.skippedEndpoints = skippedEndpoints;
        this.authEndpoints = authEndpoints;
        this.endpointsForAuth = endpointsForAuth;
    }

    /**
     * this method is used to collect all objects in sut
     * @param type is the type schema of the param for an object
     * @param param is the concrete param example
     *              note that multiple params could belong to the same type schema
     * @param isTypeToIdentify is the type to identify which likely does not exist in client library
     */
    public void registerType(TypeSchema type, NamedTypedValue param, boolean isTypeToIdentify){
        if (isTypeToIdentify){
            if (!(type instanceof CycleObjectType)){
                NamedTypedValue r = identifiedResponseTypes.get(param.getType().getFullTypeNameWithGenericType());
                if (r == null || param.getType().depth > r.getType().depth)
                    identifiedResponseTypes.put(param.getType().getFullTypeNameWithGenericType(), param);
            }
        }else{
            String typeName = type.getFullTypeNameWithGenericType();
            if (!(type instanceof CycleObjectType)){
                TypeSchema t = typeCollections.get(typeName);
                if (t == null || t.depth < type.depth)
                    typeCollections.put(typeName, type);
            }
            if (!(param.getType() instanceof CycleObjectType)){
                NamedTypedValue p = objParamCollections.get(param.getType().getFullTypeNameWithGenericType());
                if (p == null || param.getType().depth > p.getType().depth)
                    objParamCollections.put(param.getType().getFullTypeNameWithGenericType(), param);
            }
        }
    }

    public Map<String, NamedTypedValue> getObjParamCollections() {
        return objParamCollections;
    }

    public TypeSchema getTypeOrNull(String name){
        return typeCollections.get(name);
    }

    public List<EndpointSchema> getEndpoints(){
        return endpoints;
    }

    public RPCType getRpcType() {
        return rpcType;
    }

    /**
     * find endpoints based on the name
     * note that [endpoints] and [endpointsForAuth] contains all endpoints could be invoked in this interface
     * @param name is the name of an endpoint
     * @return a list of endpoints based on the specified name
     */
    public List<EndpointSchema> findEndpoints(String name){
        List<EndpointSchema> found = endpoints.stream().filter(s-> s.getName().equals(name)).collect(Collectors.toList());
        if (found.isEmpty() && endpointsForAuth!=null && !endpointsForAuth.isEmpty())
            return endpointsForAuth.stream().filter(s-> s.getName().equals(name)).collect(Collectors.toList());
        return found;
    }

    /**
     *
     * @param dto is a rpc action dto
     * @return one endpoint based on an action dto
     * note that there should only exist one endpoint which conforms with the specified dto.
     */
    public EndpointSchema getOneEndpoint(RPCActionDto dto){
        List<EndpointSchema> list = endpoints.stream().filter(s-> s.sameEndpoint(dto)).collect(Collectors.toList());

        if (list.isEmpty()){
            list.addAll(endpointsForAuth.stream().filter(s-> s.sameEndpoint(dto)).collect(Collectors.toList()));
        }

        if (list.size() == 1)
            return list.get(0);

        if (list.size() > 1)
            throw new RuntimeException("ERROR: there exists more than 1 endpoint which conforms with the specified dto "+dto.descriptiveInfo());

        throw new RuntimeException("ERROR: there does not exist any endpoint which conforms with the specified dto " + dto.descriptiveInfo());
    }


    /**
     * find an endpoint in this interface with seeded schema
     * @param dto the seeded rpc action dto
     * @return an endpoint schema
     */
    public EndpointSchema getOneEndpointWithSeededDto(SeededRPCActionDto dto){
        List<EndpointSchema> list = endpoints.stream().filter(s-> s.sameEndpoint(dto)).collect(Collectors.toList());

        if (list.size() == 1)
            return list.get(0);

        if (list.size() > 1)
            throw new RuntimeException("ERROR: there exists more than 1 endpoint which conforms with the specified seeded test dto "+dto.descriptiveInfo());

        if (skippedEndpoints.contains(dto.functionName)){
            SimpleLogger.uniqueWarn("Fail to handle the ");
            return null;
        }

        throw new RuntimeException("ERROR: there does not exist any endpoint which conforms with the specified seeded test dto "+dto.descriptiveInfo());
    }

    public String getName() {
        return name;
    }

    public String getClientInfo(){
        return clientInfo;
    }

    public Map<String, TypeSchema> getTypeCollections() {
        return typeCollections;
    }

    /**
     *
     * @return a dto of the RPC interface schema which would be sent to core as a part of sut info
     */
    public RPCInterfaceSchemaDto getDto(){
        RPCInterfaceSchemaDto dto = new RPCInterfaceSchemaDto();
        dto.types = objParamCollections.values().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        dto.interfaceId = this.getName();
        dto.clientInfo = this.getClientInfo();
        dto.endpoints = endpoints.stream().map(EndpointSchema::getDto).collect(Collectors.toList());
        if (skippedEndpoints != null)
            dto.skippedEndpoints = new ArrayList<>(skippedEndpoints);
        if (authEndpoints!= null && !authEndpoints.isEmpty()){
            dto.authEndpointReferences = new ArrayList<>();
            dto.authEndpoints = new ArrayList<>();
            authEndpoints.forEach((k, v)->{
                dto.authEndpointReferences.add(k);
                dto.authEndpoints.add(v.getDto());
            });
        }
        if (!identifiedResponseTypes.isEmpty())
            dto.identifiedResponseTypes = identifiedResponseTypes.values().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        return dto;
    }
}
