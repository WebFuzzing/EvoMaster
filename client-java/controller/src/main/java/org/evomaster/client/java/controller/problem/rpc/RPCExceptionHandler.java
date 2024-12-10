package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.api.dto.ActionResponseDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCExceptionInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionCategory;
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionType;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

/**
 * handle RPC exception, for instance
 * - extract possible category eg, application, protocol, if possible
 * - extract exception info, eg, customized exception, message, or status code
 */
public class RPCExceptionHandler {

    private final static String THRIFT_EXCEPTION_ROOT= "org.apache.thrift.TException";

    /**
     *
     * @param e is an exception instance thrown after the endpoint invocation
     * @param dto represents the endpoint which was invoked
     * @param endpointSchema is the schema of the endpoint
     * @param type is the RPC type
     */
    public static void handle(Object e, ActionResponseDto dto, EndpointSchema endpointSchema, RPCType type, Map<Class, Integer> levelsMap){

        Object exceptionToHandle = e;
        boolean isCause = false;
        // handle undeclared throwable exception
        if (UndeclaredThrowableException.class.isAssignableFrom(e.getClass())){
            Object cause = getExceptionCause(e);
            if (cause != null){
                exceptionToHandle = cause;
                isCause = true;
            }
        }

        boolean handled = false;
        RPCExceptionInfoDto exceptionInfoDto = null;
        try {
            exceptionInfoDto = handleExceptionNameAndMessage(exceptionToHandle);

            handleImportanceLevels(exceptionToHandle, exceptionInfoDto, levelsMap);

            handled = handleDefinedException(exceptionToHandle, endpointSchema, type, exceptionInfoDto);

            // Thrift exception
            if (isFromTException(e)){
                handled = handleThrift(exceptionToHandle, endpointSchema, exceptionInfoDto) || handled;
            }

            if (handled) {
                dto.exceptionInfoDto = exceptionInfoDto;
                dto.exceptionInfoDto.isCauseOfUndeclaredThrowable = isCause;
                return;
            }
        } catch (ClassNotFoundException ex) {
            dto.exceptionInfoDto = exceptionInfoDto;
            throw new RuntimeException("ERROR: fail to handle defined exception for "+type+" with error msg:"+ ex);
        }


        // handling defined exception for each RPC
//        switch (type){
//            case THRIFT: handled = handleThrift(exceptionToHandle, endpointSchema, exceptionInfoDto); break;
//            case GENERAL: break; // do nothing
//            default: throw new RuntimeException("ERROR: NOT SUPPORT exception handling for "+type);
//        }

        handleUnexpectedException(exceptionToHandle, exceptionInfoDto);

        dto.exceptionInfoDto = exceptionInfoDto;

        dto.exceptionInfoDto.isCauseOfUndeclaredThrowable = isCause;
    }

    private static void handleImportanceLevels(Object e, RPCExceptionInfoDto dto, Map<Class, Integer> levelsMap) {
        if (levelsMap == null) return;
        Integer level = levelsMap.get(e.getClass());
        if (level != null)
            dto.importanceLevel = level;
    }

    private static void handleUnexpectedException(Object e, RPCExceptionInfoDto dto){

        dto.type = RPCExceptionType.UNEXPECTED_EXCEPTION;

    }

    private static boolean isFromTException(Object e){
        try {
            return Class.forName(THRIFT_EXCEPTION_ROOT).isAssignableFrom(e.getClass());
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static RPCExceptionInfoDto handleExceptionNameAndMessage(Object e){
        RPCExceptionInfoDto dto = new RPCExceptionInfoDto();

        if (Exception.class.isAssignableFrom(e.getClass())){
            dto.exceptionName = e.getClass().getName();
            dto.exceptionMessage = getExceptionMessage(e);
        }else
            SimpleLogger.error("ERROR: the exception is not java.lang.Exception "+e.getClass().getName());

        return dto;
    }



    /**
     * handle exceptions from thrift
     * https://javadoc.io/doc/org.apache.thrift/libthrift/latest/org/apache/thrift/TException.html
     * @param e is an exception thrown from the rpc call execution
     * @param endpointSchema is the schema of this endpoint
     * @return extracted exception dto
     */
    private static boolean handleThrift(Object e, EndpointSchema endpointSchema, RPCExceptionInfoDto dto)  {
        boolean handled = false;
        try {
            if (!isRootThriftException(e)){
                //SimpleLogger.info("Exception e is not an instance of TException of Thrift, and it is "+ e.getClass().getName());
                return false;
            }

            handled = handleTException(e, dto);

            if (!handled){
                SimpleLogger.uniqueWarn("Fail to extract exception type info for an exception "+ e.getClass().getName());
            }

        } catch (ClassNotFoundException ex) {
            SimpleLogger.error("ERROR: in handling Thrift exception with error msg:", ex);
            //throw new IllegalStateException("ERROR: in handling Thrift exception with error msg:"+ex.getMessage());
        }

        return handled;
    }


    private static boolean handleDefinedException(Object e, EndpointSchema endpointSchema, RPCType rpcType, RPCExceptionInfoDto dto) throws ClassNotFoundException {

        if (endpointSchema.getExceptions() == null) return false;

        for (NamedTypedValue p : endpointSchema.getExceptions()){
            String type = p.getType().getFullTypeNameWithGenericType();
            // skip to handle root TException here
            if (type.equals(THRIFT_EXCEPTION_ROOT))
                continue;
            if (isInstanceOf(e, type)){
                p.setValueBasedOnInstance(e);
                dto.exceptionDto = p.getDto();
                dto.type = RPCExceptionType.CUSTOMIZED_EXCEPTION;
                return true;
            }
        }
        return false;
    }

    private static boolean handleTException(Object e, RPCExceptionInfoDto dto)  {

        Method getType = null;
        try {
            getType = e.getClass().getDeclaredMethod("getType");
            getType.setAccessible(true);
            int type = (int) getType.invoke(e);

            dto.type = getExceptionType(extract(e), type);
            return true;
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException ex) {
            /*
                with old version of TException, eg thrift 0.9.3
                there is no getType method, then in this case, we set type based on the exception class name
             */
            SimpleLogger.error("Fail to get type of TException with getType() ", ex);
        }
        return false;
    }

    private static String getExceptionMessage(Object e)  {
        Method getMessage = null;
        try {
            getMessage = e.getClass().getMethod("getMessage");
            getMessage.setAccessible(true);
            return (String) getMessage.invoke(e);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            SimpleLogger.error("Error: fail to get message of the exception with ", ex);
            return null;
        }
    }


    private static Object getExceptionCause(Object e)  {
        Method getCause = null;
        try {
            getCause = e.getClass().getMethod("getCause");
            getCause.setAccessible(true);
            Object exp = getCause.invoke(e);
            if (exp != null) return exp;

            getCause = e.getClass().getMethod("getUndeclaredThrowable");
            getCause.setAccessible(true);
            return getCause.invoke(e);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            SimpleLogger.error("Error: fail to get message of the exception with ", ex);
            return null;
        }
    }

    private static boolean isRootThriftException(Object e) throws ClassNotFoundException {
        return Class.forName(THRIFT_EXCEPTION_ROOT).isInstance(e);
    }


    private static boolean isInstanceOf(Object e, String name) throws ClassNotFoundException {
        return Class.forName(name).isInstance(e);
    }

    /**
     * Note that now we only support categorize exception for thrift
     * @param e is the exception instance
     * @return a category
     * @throws ClassNotFoundException could not find the TException of the thrift
     */
    private static RPCExceptionCategory extract(Object e) throws ClassNotFoundException {
        if (isInstanceOf(e, "org.apache.thrift.TApplicationException"))
            return RPCExceptionCategory.APPLICATION;
        if (isInstanceOf(e, "org.apache.thrift.protocol.TProtocolException"))
            return RPCExceptionCategory.PROTOCOL;
        if (isInstanceOf(e, "org.apache.thrift.transport.TTransportException"))
            return RPCExceptionCategory.TRANSPORT;
        return RPCExceptionCategory.OTHERS;
    }

    private static RPCExceptionType getExceptionType(RPCExceptionCategory category, int intValue){
        for (RPCExceptionType type: RPCExceptionType.values()){
            if (type.intValue == intValue && type.category == category) return type;
        }
        return null;
    }
}
