package org.evomaster.client.java.controller.problem;

import java.util.Map;
import java.util.Set;

/**
 * created by manzhang on 2021/11/3
 */
public class RPCProblem implements ProblemInfo{

    /**
     * a list of interfaces in the RPC service
     */
    private final Map<String, Object> mapOfInterfaceAndClient;

    private final RPCType type;

    public RPCProblem(Map<String, Object> mapOfInterfaceAndClient) {
        this(mapOfInterfaceAndClient, RPCType.THRIFT);
    }

    public RPCProblem(Map<String, Object> mapOfInterfaceAndClient, RPCType type) {
        this.mapOfInterfaceAndClient = mapOfInterfaceAndClient;
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
