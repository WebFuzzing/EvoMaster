package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * created by manzhang on 2021/11/27
 */
public enum RPCSupportedDataType {
    P_INT, P_BYTE, P_SHORT, P_LONG, P_FLOAT, P_DOUBLE, P_BOOLEAN, P_CHAR,
    INT, BYTE, SHORT, LONG, FLOAT, DOUBLE, BOOLEAN, CHAR,
    STRING,
    ENUM,
    ARRAY, LIST, SET, MAP,
    UTIL_DATE,
    PAIR, // for map
    BYTEBUFFER, //Thrift
    CUSTOM_OBJECT,
    CUSTOM_CYCLE_OBJECT
}
