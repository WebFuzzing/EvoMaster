package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.api.dto.ActionResponseDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCExceptionInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;

public class RPCExceptionHandler {

    private final static String THRIFT_EXCEPTION_ROOT= "org.apache.thrift.TException";

    public static void handle(Exception e, ActionResponseDto dto, EndpointSchema endpointSchema, RPCType type){

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
    private static RPCExceptionInfoDto handleThrift(Exception e, EndpointSchema endpointSchema)  {
        RPCExceptionInfoDto dto = new RPCExceptionInfoDto();

        try {
            if (!Class.forName(THRIFT_EXCEPTION_ROOT).isInstance(e)){
                throw new RuntimeException("ERROR: exception e is not an instance of TException of Thrift, and it is "+ e.getClass().getName());
            }
            //org.apache.thrift.transport.TException



            //org.apache.thrift.TApplicationException

            //org.apache.thrift.protocol.TProtocolException

        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return dto;
    }

}
