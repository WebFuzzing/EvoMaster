package org.evomaster.client.java.controller.problem.rpc;

import java.lang.reflect.Type;

/**
 * this class presents extracted info of a field of protobuf dto
 */
public class Protobuf3Field {

    /**
     * name
     */
    public String fieldName;

    /**
     * type
     */
    public Class<?> fieldType;

    /**
     * generic type if it exists, such as for Map, List
     */
    public Type genericType;

    /**
     * name of corresponding setter
     */
    public String setterName;

    /**
     * types of input parameters of the setter
     *
     * the input parameter might not be consistent with the fieldType in gRPC
     * the type could be defined with its supertype
     * then we need such explicit info in order to get the setter
     *
     * For instance, the fieldType is List, and the input type is Iterator
     *
     */
    public Class<?>[] setterInputParams;

    /**
     * name of getter
     */
    public String getterName;

    public Protobuf3Field(){}
}
