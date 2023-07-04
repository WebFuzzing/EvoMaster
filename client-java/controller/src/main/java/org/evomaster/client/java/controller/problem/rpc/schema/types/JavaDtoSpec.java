package org.evomaster.client.java.controller.problem.rpc.schema.types;

/**
 * supported specification of Java Dto
 *
 * the specification would relate to how to construct dto
 */
public enum JavaDtoSpec {
    /**
     * pure Java
     */
    DEFAULT,

    /**
     * using proto3
     *
     * if the specification is proto3,
     * then dto is needed to construct with the builder
     * see <a href="https://protobuf.dev/reference/java/java-generated/">info of Java Generated Code</a>
     *
     */
    PROTO3
}
