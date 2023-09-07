package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockRPCExternalServiceDto {

    /**
     * a key refers to the RPC-based API which contains the specified RPC interface
     */
    public String appKey;

    /**
     * a full name of the RPC interface to mock
     */
    public String interfaceFullName;

    /**
     * a function of the interface if it is invoked
     */
    public String functionName;

    /**
     * a list of types of input parameters of the function
     */
    public List<String> inputParameterTypes;

    /**
     * a list of requests that are used to specify the rules if they have
     *
     * the param is nullable, if it is null, the size of [responses] would be 1,
     * then always return the specified one
     *
     * if there exist rules to return responses based on a given request,
     * then specify possible requests with corresponding responses, by using [requests] and [responses]
     * Note that request and response will be matched based on index of the list
     */
    public List<String> requestRules;


    /**
     * a list of responses to return
     *
     * there could exist multiples if there exist rules
     */
    public List<String> responses;


    /**
     * type info of the responses
     */
    public List<String> responseTypes;


    /**
     * explict type info with generics
     * the info is used by evomaster
     */
    public List<String> responseFullTypesWithGeneric;


    public MockRPCExternalServiceDto copy(){
        MockRPCExternalServiceDto copy = new MockRPCExternalServiceDto();
        copy.appKey = appKey;
        copy.interfaceFullName = interfaceFullName;
        copy.functionName = functionName;
        if (inputParameterTypes != null)
            copy.inputParameterTypes = new ArrayList<>(inputParameterTypes);
        if (responses != null)
            copy.responses = new ArrayList<>(responses);
        if (requestRules != null)
            copy.requestRules = new ArrayList<>(requestRules);
        if (responseTypes != null)
            copy.responseTypes = new ArrayList<>(responseTypes);
        if (responseFullTypesWithGeneric != null){
            copy.responseFullTypesWithGeneric = new ArrayList<>(responseFullTypesWithGeneric);
        }

        return copy;
    }
}
