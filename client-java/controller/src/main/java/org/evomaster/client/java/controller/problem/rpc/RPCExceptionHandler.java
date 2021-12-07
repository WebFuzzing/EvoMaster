package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.api.dto.ActionResponseDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCExceptionInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionCategory;
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionType;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RPCExceptionHandler {

    private final static String THRIFT_EXCEPTION_ROOT= "org.apache.thrift.TException";

    public static void handle(Object e, ActionResponseDto dto, EndpointSchema endpointSchema, RPCType type){

        switch (type){
            case THRIFT: dto.exceptionInfoDto = handleThrift(e, endpointSchema);
            default: throw new RuntimeException("ERROR: NOT SUPPORT exception handling for "+type);
        }
    }

    /**
     * handle exceptions from thrift
     * https://javadoc.io/doc/org.apache.thrift/libthrift/latest/org/apache/thrift/TException.html
     * @param e is an exception thrown from the rpc call execution
     * @param endpointSchema is the schema of this endpoint
     * @return extracted exception dto
     */
    private static RPCExceptionInfoDto handleThrift(Object e, EndpointSchema endpointSchema)  {
        RPCExceptionInfoDto dto = new RPCExceptionInfoDto();

        try {
            if (!isRootThriftException(e)){
                throw new RuntimeException("ERROR: exception e is not an instance of TException of Thrift, and it is "+ e.getClass().getName());
            }

            ParamDto edto = handleDefinedException(e, endpointSchema);
            dto.exceptionDto = edto;

            handleTException(e, dto);
            if (dto.type == null){
                SimpleLogger.error("Fail to extract exception type info for an exception "+ e.getClass().getName());
            }

        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |  IllegalAccessException ex) {
            throw new IllegalStateException("ERROR: in handling Thrift exception with error msg:"+ex.getMessage());
        }

        return dto;
    }


    private static ParamDto handleDefinedException(Object e, EndpointSchema endpointSchema) throws ClassNotFoundException {
        for (NamedTypedValue p : endpointSchema.getExceptions()){
            if (isRootThriftException(e)) continue;
            if (isInstanceOf(e, p.getType().getFullTypeName())){
                p.setValueBasedOnInstance(e);
                return p.getDto();
            }
        }
        return null;
    }

    private static void handleTException(Object e, RPCExceptionInfoDto dto) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Method getMessage = e.getClass().getDeclaredMethod("getMessage");
        getMessage.setAccessible(true);
        String msg =  (String) getMessage.invoke(e);
        dto.exceptionMessage = msg;

        Method getType = e.getClass().getDeclaredMethod("getType");
        getType.setAccessible(true);
        int type = (int) getType.invoke(e);

        dto.type = getExceptionType(extract(e), type);

    }

    private static boolean isRootThriftException(Object e) throws ClassNotFoundException {
        return Class.forName(THRIFT_EXCEPTION_ROOT).isInstance(e);
    }

    private static boolean isInstanceOf(Object e, String name) throws ClassNotFoundException {
        return Class.forName(name).isInstance(e);
    }

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
