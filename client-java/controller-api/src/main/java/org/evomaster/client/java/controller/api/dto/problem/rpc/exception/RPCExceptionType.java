package org.evomaster.client.java.controller.api.dto.problem.rpc.exception;


import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;

/**
 * currently, we only collect thrift
 */
public enum RPCExceptionType {

    CUSTOMIZED_EXCEPTION(RPCExceptionCategory.OTHERS, new RPCType[]{RPCType.THRIFT}, -1),

    /**
     * this exception is thrown during execution, but not declared in the endpoint
     * this case is identified as a bug,
     * eg, runtime exception which is unchecked
     *      https://docs.oracle.com/javase/8/docs/api/java/lang/RuntimeException.html
     *
     * category might be application (currently set as Others),
     *         however, need a clear definition for each category
     *
     */
    UNEXPECTED_EXCEPTION(RPCExceptionCategory.OTHERS, new RPCType[]{RPCType.GENERAL}, -1),

    /*
        https://javadoc.io/doc/org.apache.thrift/libthrift/latest/org/apache/thrift/TApplicationException.html
     */
    APP_BAD_SEQUENCE_ID(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 4),
    APP_INTERNAL_ERROR(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 6),
    APP_INVALID_MESSAGE_TYPE(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 2),
    APP_INVALID_PROTOCOL(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 9),
    APP_INVALID_TRANSFORM(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 8),
    APP_MISSING_RESULT(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 5),
    APP_PROTOCOL_ERROR(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 7),
    APP_UNKNOWN(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 0),
    APP_UNKNOWN_METHOD(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 1),
    APP_UNSUPPORTED_CLIENT_TYPE(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT}, 10),
    APP_WRONG_METHOD_NAME(RPCExceptionCategory.APPLICATION, new RPCType[]{RPCType.THRIFT},3),

    /*
    https://javadoc.io/doc/org.apache.thrift/libthrift/latest/org/apache/thrift/transport/TTransportException.html
     */
    TRANS_ALREADY_OPEN(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}, 2),
    TRANS_CORRUPTED_DATA(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}, 5),
    TRANS_END_OF_FILE(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}, 4),
    TRANS_NOT_OPEN(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}, 1),
    TRANS_TIMED_OUT(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}, 3),
    TRANS_UNKNOWN(RPCExceptionCategory.TRANSPORT, new RPCType[]{RPCType.THRIFT}, 0),

    /*
    https://javadoc.io/doc/org.apache.thrift/libthrift/latest/org/apache/thrift/protocol/TProtocolException.html
     */
    PROTO_BAD_VERSION(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}, 4),
    PROTO_DEPTH_LIMIT(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}, 6),
    PROTO_INVALID_DATA(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}, 1),
    PROTO_NEGATIVE_SIZE(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}, 2),
    PROTO_NOT_IMPLEMENTED(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}, 5),
    PROTO_SIZE_LIMIT(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}, 3),
    PROTO_UNKNOWN(RPCExceptionCategory.PROTOCOL, new RPCType[]{RPCType.THRIFT}, 0)
    ;

    public final RPCType[] supportedRPC;
    public final RPCExceptionCategory category;
    public final int intValue;

    RPCExceptionType(RPCExceptionCategory category, RPCType[] types, int intValue){
        this.category = category;
        this.supportedRPC = types;
        this.intValue = intValue;
    }
}
