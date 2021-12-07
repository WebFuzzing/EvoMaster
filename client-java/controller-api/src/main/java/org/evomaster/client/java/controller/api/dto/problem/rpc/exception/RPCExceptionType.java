package org.evomaster.client.java.controller.api.dto.problem.rpc.exception;


import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;

/**
 * currently, we only collect thrift
 */
public enum RPCExceptionType {
    /*
        https://javadoc.io/doc/org.apache.thrift/libthrift/latest/org/apache/thrift/TApplicationException.html
     */
    APP_BAD_SEQUENCE_ID(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_INTERNAL_ERROR(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_INVALID_MESSAGE_TYPE(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_INVALID_PROTOCOL(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_INVALID_TRANSFORM(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_MISSING_RESULT(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_PROTOCOL_ERROR(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_UNKNOWN(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_UNKNOWN_METHOD(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_UNSUPPORTED_CLIENT_TYPE(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),
    APP_WRONG_METHOD_NAME(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}),

    /*
    https://javadoc.io/doc/org.apache.thrift/libthrift/latest/org/apache/thrift/transport/TTransportException.html
     */
    TRANS_ALREADY_OPEN(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}),
    TRANS_CORRUPTED_DATA(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}),
    TRANS_END_OF_FILE(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}),
    TRANS_NOT_OPEN(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}),
    TRANS_TIMED_OUT(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}),
    TRANS_UNKNOWN(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}),

    /*
    https://javadoc.io/doc/org.apache.thrift/libthrift/latest/org/apache/thrift/protocol/TProtocolException.html
     */
    PROTO_BAD_VERSION(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}),
    PROTO_DEPTH_LIMIT(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}),
    PROTO_INVALID_DATA(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}),
    PROTO_NEGATIVE_SIZE(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}),
    PROTO_NOT_IMPLEMENTED(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}),
    PROTO_SIZE_LIMIT(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}),
    PROTO_UNKNOWN(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT})
    ;

    public final RPCType[] supportedRPC;
    public final RPCExceptionCategory category;

    RPCExceptionType(RPCExceptionCategory category, RPCType[] types){
        this.category = category;
        this.supportedRPC = types;
    }
}
