package org.evomaster.client.java.controller.problem;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;

import java.util.*;

/**
 * define RPCProblem used in driver
 */
public class RPCProblem extends ProblemInfo{

    /**
     * a map of interfaces with corresponding client
     * Key - a full name of the interface
     * Value - an instance of the client
     */
    private final Map<String, Object> mapOfInterfaceAndClient;

    /**
     * the type of the interface
     */
    private final RPCType type;

    /**
     * a map of endpoints to be skipped for each interface
     * Key - name of interface
     * Value - a list of names of endpoints to be skipped
     */
    private final Map<String, List<String>> skipEndpointsByName;

    /**
     * a map of endpoints to be skipped for each interface
     * Key - name of the interface
     * Value - a list of annotations. endpoints are skipped if they applied the annotation
     */
    private final Map<String, List<String>> skipEndpointsByAnnotation;

    /**
     * a map of endpoints to be involved for each interface
     * Key - name of interface
     * Value - a list of names of endpoints to be involved
     */
    private final Map<String, List<String>> involveEndpointsByName;

    /**
     * a map of endpoints to be involved for each interface
     * Key - name of the interface
     * Value - a list of annotations. endpoints are involved if they applied the annotation
     */
    private final Map<String, List<String>> involveEndpointsByAnnotation;


    /**
     *
     * @param interfaceClass  an interface with API definition
     * @param client          an actual client library for the API
     * @param type            the type of RPC system
     */
    public <T,K extends T> RPCProblem(Class<T> interfaceClass, K client, RPCType type){
        this(new HashMap<String,Object>(){{put(interfaceClass.getName(),client);}},type);
    }

    /**
     *
     * @param mapOfInterfaceAndClient a map of interfaces with their corresponding client
     */
    public RPCProblem(Map<String, Object> mapOfInterfaceAndClient) {
        this(mapOfInterfaceAndClient, RPCType.GENERAL);
    }

    /**
     *
     * @param mapOfInterfaceAndClient a map of interfaces with their corresponding client
     * @param type is RPC type
     */
    public RPCProblem(Map<String, Object> mapOfInterfaceAndClient, RPCType type) {
        this(mapOfInterfaceAndClient, null, null, null, null, type);
    }

    /**
     *
     * @param mapOfInterfaceAndClient a map of interfaces with their corresponding client
     * @param skipEndpointsByName is a map of endpoints to be skipped for each interface by endpoint name
     * @param skipEndpointsByAnnotation is a map of endpoints to be skipped for each interface by endpoint annotation
     * @param involveEndpointsByName is a map of endpoints to be involved for each interface by endpoint name
     * @param involveEndpointsByAnnotation is a map of endpoints to be involved for each interface by endpoint annotation
     * @param type is RPC type
     */
    public RPCProblem(Map<String, Object> mapOfInterfaceAndClient,
                      Map<String, List<String>> skipEndpointsByName,
                      Map<String, List<String>> skipEndpointsByAnnotation,
                      Map<String, List<String>> involveEndpointsByName,
                      Map<String, List<String>> involveEndpointsByAnnotation,
                      RPCType type){
        this.mapOfInterfaceAndClient = mapOfInterfaceAndClient == null ? new HashMap<>() : new HashMap<>(mapOfInterfaceAndClient);
        this.involveEndpointsByAnnotation = involveEndpointsByAnnotation == null ? new HashMap<>() : new HashMap<>(involveEndpointsByAnnotation);
        this.involveEndpointsByName = involveEndpointsByName == null ? new HashMap<>() : new HashMap<>(involveEndpointsByName);
        this.skipEndpointsByAnnotation = skipEndpointsByAnnotation == null ? new HashMap<>() : new HashMap<>(skipEndpointsByAnnotation);
        this.skipEndpointsByName = skipEndpointsByName == null ? new HashMap<>() : new HashMap<>(skipEndpointsByName);
        this.type = type;

    }

    /**
     *
     * @return a set of interface names for the RPC problem
     */
    public Set<String> getKeysOfMapOfInterfaceAndClient() {
        return mapOfInterfaceAndClient.keySet();
    }

    /**
     * @return type of the RPC interface
     */
    public RPCType getType() {
        return type;
    }

    public Map<String, Object> getMapOfInterfaceAndClient() {
        return Collections.unmodifiableMap(mapOfInterfaceAndClient);
    }

    public Map<String, List<String>> getSkipEndpointsByName() {
        return Collections.unmodifiableMap(skipEndpointsByName);
    }

    public Map<String, List<String>> getSkipEndpointsByAnnotation() {
        return Collections.unmodifiableMap(skipEndpointsByAnnotation);
    }

    public Map<String, List<String>> getInvolveEndpointsByName() {
        return Collections.unmodifiableMap(involveEndpointsByName);
    }

    public Map<String, List<String>> getInvolveEndpointsByAnnotation() {
        return Collections.unmodifiableMap(involveEndpointsByAnnotation);
    }

    /**
     *
     * @param interfaceId is the full name of the interface
     * @return the client based on the interface name
     */
    public Object getClient(String interfaceId){
        return mapOfInterfaceAndClient.get(interfaceId);
    }


    @Override
    public RPCProblem withServicesToNotMock(List<ExternalService> servicesToNotMock){
        RPCProblem p =  new RPCProblem(this.mapOfInterfaceAndClient,
                this.skipEndpointsByName,
                this.skipEndpointsByAnnotation,
                this.involveEndpointsByName,
                this.involveEndpointsByAnnotation,
                this.type);
        p.servicesToNotMock.addAll(servicesToNotMock);
        return p;
    }
}
