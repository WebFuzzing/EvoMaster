package org.evomaster.client.java.controller.problem;

import java.util.List;

/**
 * created by manzhang on 2021/11/3
 */
public class RPCProblem implements ProblemInfo{

    /**
     * a list of interfaces in the RPC service
     */
    private final List<String> interfaceDefinitions;

    private final RPCType type;

    public RPCProblem(List<String> interfaceDefinitions) {
        this(interfaceDefinitions, RPCType.THRIFT);
    }

    public RPCProblem(List<String> interfaceDefinitions, RPCType type) {
        this.interfaceDefinitions = interfaceDefinitions;
        this.type = type;
    }

    public List<String> getInterfaceDefinitions() {
        return interfaceDefinitions;
    }

    public RPCType getType() {
        return type;
    }
}
