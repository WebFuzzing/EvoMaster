package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.api.dto.problem.rpc.*;
import org.evomaster.client.java.controller.problem.RPCType;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * created by manzhang on 2021/11/4
 */
public class RPCEndpointsBuilder {

    /**
     *
     * @param interfaceName the name of interface
     * @param type is the type of RPC, e.g., gRPC, Thrift
     * @return an interface schema for evomaster to access
     */
    public static InterfaceSchema build(String interfaceName, RPCType type){
        List<EndpointSchema> endpoints = new ArrayList<>();
        InterfaceSchema schema = new InterfaceSchema(interfaceName, endpoints);

        return schema;
    }

    private static EndpointSchema build(Method method, RPCType type){
        List<ParamSchema> requestParams = new ArrayList<>();
//        EndpointSchema endpoint = new EndpointSchema(method.getName())

        return null;
    }

    private static ParamSchema build(Parameter parameter){
        String name = parameter.getName();
        Class<?> clazz = parameter.getType();
        return build(clazz, name);
    }

    private static ParamSchema build(Class<?> clazz, String name){
       if (PrimitiveOrWrapperParamSchema.isPrimitiveOrTypes(clazz)){
            return new PrimitiveOrWrapperParamSchema(clazz.getSimpleName(), name);
       }else if (clazz.isArray()){

       }else if (clazz.isEnum()){
           return new EnumParamSchema(name, Arrays.stream(clazz.getEnumConstants()).map(Object::toString).collect(Collectors.toList()));
       }else if (clazz == String.class){
           return new StringParamSchema(name);
       }else if(clazz.isAssignableFrom(Collection.class)){

       }else {
            // handle object
       }

       return null;
    }
}
