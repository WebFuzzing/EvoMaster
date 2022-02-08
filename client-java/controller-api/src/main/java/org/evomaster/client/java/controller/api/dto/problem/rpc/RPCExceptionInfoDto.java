package org.evomaster.client.java.controller.api.dto.problem.rpc;

import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionType;

/**
 * contain exception info
 */
public class RPCExceptionInfoDto {

    /**
     * class name of the exception
     */
    public String exceptionName;

    /**
     * message of the exception
     */
    public String exceptionMessage;

    /**
     * type of the exception
     */
    public RPCExceptionType type;

    /**
     * a dto of the exception defined by the RPC service
     */
    public ParamDto exceptionDto;

    /**
     * if the exception is a cause of java.lang.UndeclaredThrowableException
     */
    public boolean isCauseOfUndeclaredThrowable;
}
