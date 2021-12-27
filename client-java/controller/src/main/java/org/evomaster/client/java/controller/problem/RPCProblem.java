package org.evomaster.client.java.controller.problem;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * created by manzhang on 2021/11/3
 */
public class RPCProblem implements ProblemInfo{

    /**
     * a list of interfaces in the RPC service
     */
    public final Map<String, Object> mapOfInterfaceAndClient;

    public final RPCType type;

    public final Map<String, List<String>> skipEndpointsByName;
    public final Map<String, List<String>> skipEndpointsByAnnotation;
    public final Map<String, List<String>> involveEndpointsByName;
    public final Map<String, List<String>> involveEndpointsByAnnotation;

    public RPCProblem(Map<String, Object> mapOfInterfaceAndClient) {
        this(mapOfInterfaceAndClient, RPCType.THRIFT);
    }

    public RPCProblem(Map<String, Object> mapOfInterfaceAndClient, RPCType type) {
        this(mapOfInterfaceAndClient, null, null, null, null, type);
    }

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

    public Set<String> getMapOfInterfaceAndClient() {
        return mapOfInterfaceAndClient.keySet();
    }

    public RPCType getType() {
        return type;
    }

    public Object getClient(String interfaceId){
        return mapOfInterfaceAndClient.get(interfaceId);
    }
}
