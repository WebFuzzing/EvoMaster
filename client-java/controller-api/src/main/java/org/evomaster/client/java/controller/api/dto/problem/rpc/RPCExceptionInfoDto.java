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
     * importance level
     * lower value more important
     * 0 represents the most important exception which should be avoided and need to be fixed earliest
     * a negative value (less than 0) represents undefined
     */
    public int importanceLevel;

    /**
     * a dto of the exception defined by the RPC service
     */
    public ParamDto exceptionDto;

    /**
     * https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/UndeclaredThrowableException.html
     * java.lang.UndeclaredThrowableException could be thrown by an endpoint invocation
     * in order to further analyze the exact exception, we analyze its cause.
     * This attribute represents whether this RPCExceptionInfoDto is based on the cause of UndeclaredThrowableException
     *
     */
    public boolean isCauseOfUndeclaredThrowable;
}
