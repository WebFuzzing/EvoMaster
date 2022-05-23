package org.evomaster.client.java.controller.problem;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * define RPCProblem used in driver
 */
public class RPCProblem implements ProblemInfo{

    /**
     * a map of interfaces with corresponding client
     * Key - a full name of the interface
     * Value - an instance of the client
     */
    public final Map<String, Object> mapOfInterfaceAndClient;

    /**
     * the type of the interface
     */
    public final RPCType type;

    /**
     * a map of endpoints to be skipped for each interface
     * Key - name of interface
     * Value - a list of names of endpoints to be skipped
     */
    public final Map<String, List<String>> skipEndpointsByName;

    /**
     * a map of endpoints to be skipped for each interface
     * Key - name of the interface
     * Value - a list of annotations. endpoints are skipped if they applied the annotation
     */
    public final Map<String, List<String>> skipEndpointsByAnnotation;

    /**
     * a map of endpoints to be involved for each interface
     * Key - name of interface
     * Value - a list of names of endpoints to be involved
     */
    public final Map<String, List<String>> involveEndpointsByName;

    /**
     * a map of endpoints to be involved for each interface
     * Key - name of the interface
     * Value - a list of annotations. endpoints are involved if they applied the annotation
     */
    public final Map<String, List<String>> involveEndpointsByAnnotation;


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
                      Map<String, List<String>> involveEndpointsByAnnotation, RPCType type){
        this.mapOfInterfaceAndClient = mapOfInterfaceAndClient;
        this.involveEndpointsByAnnotation = involveEndpointsByAnnotation;
        this.involveEndpointsByName = involveEndpointsByName;
        this.skipEndpointsByAnnotation = skipEndpointsByAnnotation;
        this.skipEndpointsByName = skipEndpointsByName;
        this.type = type;

    }

    /**
     *
     * @return a set of interface names for the RPC problem
     */
    public Set<String> getMapOfInterfaceAndClient() {
        return mapOfInterfaceAndClient.keySet();
    }

    /**
     * @return type of the RPC interface
     */
    public RPCType getType() {
        return type;
    }

    /**
     *
     * @param interfaceId is the full name of the interface
     * @return the client based on the interface name
     */
    public Object getClient(String interfaceId){
        return mapOfInterfaceAndClient.get(interfaceId);
    }
}
