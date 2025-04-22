package org.evomaster.client.java.controller.api.dto.problem.rpc;

public class RPCActionWithResultDto {

    /**
     * rpc action to execute
     */
    public RPCActionDto rpcAction;

    /**
     * a response return by the rpc action
     * the response is nullable if the return is null or throw any exception
     */
    public ParamDto response;

    /**
     * class name of the exception
     */
    public String exceptionName;

    /**
     * message of the exception
     */
    public String exceptionMessage;
}
