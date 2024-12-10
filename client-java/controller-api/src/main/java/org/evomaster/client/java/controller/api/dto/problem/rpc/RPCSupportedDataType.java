package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * a list of types we support for RPC
 */
public enum RPCSupportedDataType {
    /**
     *  primitive int
     */
    P_INT,

    /**
     * primitive byte
     */
    P_BYTE,

    /**
     * primitive short
     */
    P_SHORT,
    /**
     * primitive long
     */
    P_LONG,
    /**
     * primitive float
     */
    P_FLOAT,
    /**
     * primitive double
     */
    P_DOUBLE,
    /**
     * primitive boolean
     */
    P_BOOLEAN,
    /**
     * primitive boolean
     */
    P_CHAR,

    /**
     * integer
     */
    INT,
    /**
     * byte
     */
    BYTE,
    /**
     * short
     */
    SHORT,
    /**
     * long
     */
    LONG,
    /**
     * float
     */
    FLOAT,
    /**
     * double
     */
    DOUBLE,
    /**
     * boolean
     */
    BOOLEAN,
    /**
     * char
     */
    CHAR,
    /**
     * string
     */
    STRING,

    /**
     * enumeration
     */
    ENUM,

    /**
     * array
     */
    ARRAY,
    /**
     * list
     */
    LIST,
    /**
     * set
     */
    SET,
    /**
     * map
     */
    MAP,
    /**
     * java.util.Date
     */
    UTIL_DATE,

    /**
     * java.time.LocalDate
     */
    LOCAL_DATE,

    /**
     * only for map
     */
    PAIR,

    /**
     * java.nio.ByteBuffer
     * note that it is used by Thrift
     */
    BYTEBUFFER,

    /**
     * object
     */
    CUSTOM_OBJECT,

    /**
     * object which contains cycle references
     * eg, A - B - A (Cycle)
     *     A {
     *          B b;
     *     }
     *     B {
     *         A a;
     *     }
     */
    CUSTOM_CYCLE_OBJECT,


    /**
     * java.math.BigInteger
     */
    BIGINTEGER,

    /**
     *  java.math.BigDecimal
     */
    BIGDECIMAL
}
