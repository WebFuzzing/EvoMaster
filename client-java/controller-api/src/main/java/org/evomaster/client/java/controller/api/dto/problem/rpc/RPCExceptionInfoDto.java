package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * contain exception info
 */
public class RPCExceptionInfoDto {

    /**
     * name of the exception
     */
    public String exceptionName;

    /**
     * message of the exception
     */
    public String exceptionMessage;

    /**
     * presenting whether the exception is specified or caught under the endpoint
     */
    public boolean definedException;

    /**
     * a dto of the exception defined by the RPC service
     */
    public ParamDto exceptionDto;
}
