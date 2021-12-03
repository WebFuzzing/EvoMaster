package org.evomaster.client.java.controller.problem.rpc.schema;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCInterfaceSchemaDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.types.TypeSchema;

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
     * a list of available endpoints in the service
     */
    private List<EndpointSchema> endpoints;

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


    public InterfaceSchema(String name, List<EndpointSchema> endpoints) {
        this.name = name;
        this.endpoints = endpoints;
    }

    public void registerType(TypeSchema type, NamedTypedValue param){
        String typeName = type.getFullTypeName();
        if (!typeCollections.containsKey(typeName)){
            typeCollections.put(typeName, type);
            objParamCollections.put(param.getType().getFullTypeName(), param);
        }

    }

    public TypeSchema getTypeOrNull(String name){
        return typeCollections.get(name);
    }

    public List<EndpointSchema> getEndpoints(){
        return endpoints;
    }

    /**
     * @param name is a name of an endpoint
     * @return a list of endpoints based on the specified name
     */
    public List<EndpointSchema> findEndpoints(String name){
        return endpoints.stream().filter(s-> s.getName().equals(name)).collect(Collectors.toList());
    }

    /**
     *
     * @param dto is a rpc action dto
     * @return one endpoint based on an action dto
     * note that there should only exist one endpoint which conforms with the specified dto.
     */
    public EndpointSchema getOneEndpoint(RPCActionDto dto){
        List<EndpointSchema> list = endpoints.stream().filter(s-> s.sameEndpoint(dto)).collect(Collectors.toList());

        if (list.size() == 1)
            return list.get(0);

        if (list.size() > 1)
            throw new RuntimeException("ERROR: there exists more than 1 endpoint which conforms with the specified dto");

        throw new RuntimeException("ERROR: there does not exist any endpoint which conforms with the specified dto");
    }

    public String getName() {
        return name;
    }

    public Map<String, TypeSchema> getTypeCollections() {
        return typeCollections;
    }

    public RPCInterfaceSchemaDto getDto(){
        RPCInterfaceSchemaDto dto = new RPCInterfaceSchemaDto();
        dto.interfaceId = this.getName();
        List<ParamDto> typeParams = new ArrayList<>();
        dto.types = typeParams;
        dto.endpoints = endpoints.stream().map(EndpointSchema::getDto).collect(Collectors.toList());
        dto.endpoints.forEach(e-> e.interfaceId = dto.interfaceId);
        return dto;
    }
}
