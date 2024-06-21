package org.evomaster.client.java.controller.api.dto.problem.rpc;

import org.evomaster.client.java.controller.api.dto.MockDatabaseDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * a dto to collect info of endpoints to be tested
 * that is used by both core (for identifying action) and driver (for endpoint invocation) sides
 */
public class RPCActionDto {

    /**
     * name of the RPC interface
     */
    public String interfaceId;

    /**
     * name of the client
     */
    public String clientInfo;

    /**
     * a variable referring to client instance
     *
     * this info would be used in static init declaration referring
     * to an actual client instance in the generated test
     * then later, the variable could be used to process rpc function call
     *
     * eg, the variable is foo
     * public class Test{
     *     private static Client foo;
     *     ...
     *     pubic void test(){
     *         response = foo.bar(request)
     *     }
     * }
     *
     * TODO
     * Note that the current test generation is proceeded in the driver
     * if we move it to core, this info could be removed
     */
    public String clientVariable;

    /**
     * name of the action
     */
    public String actionName;

    /**
     * a list of actions for performing mocking external services
     */
    public List<MockRPCExternalServiceDto> mockRPCExternalServiceDtos;

    /**
     * a list of mock objects for database sql commands
     */
    public List<MockDatabaseDto> mockDatabaseDtos;

    /**
     * request params
     */
    public List<ParamDto> requestParams;

    /**
     * response param (nullable)
     */
    public ParamDto responseParam;

    /**
     * if the action requires auth to access
     */
    public boolean isAuthorized;

    /**
     * possible candidates for auth to access the endpoint
     */
    public List<Integer> requiredAuthCandidates;

    /**
     * related candidates to customize values in request of this endpoint
     */
    public Set<String> relatedCustomization;

    /**
     * an action to setup auth in this invocation
     */
    public RPCActionDto authSetup;


    // test generation configuration, might be removed later
    /**
     * variable name of response
     */
    public String responseVariable;

    /**
     * variable name of controller
     */
    public String controllerVariable;

    /**
     * if generate assertions on driver side and send back core
     */
    public boolean doGenerateAssertions;

    /**
     * if generate test script on driver side and send back core
     */
    public boolean doGenerateTestScript;

    /**
     * the output format
     * if doGenerateTestScript is true, outputFormat cannot be null
     * Note that the info is kept in sync with what the user specifies in driver or EMConfig
     */
    public SutInfoDto.OutputFormat outputFormat;

    /**
     * the maximum number of assertions to be generated for data in collections
     * zero or negative number means that assertions would be generated for all data in collection
     */
    public int maxAssertionForDataInCollection;

    /**
     * a list of DTOs which need to be extracted at the driver side
     * they can be eg, response of external services
     */
    public List<String> missingDto;

    /**
     *
     * @return a copy of RPCActionDto for enabling its invocation
     * eg, exclude all possible candidates of param values and auth
     */
    public RPCActionDto copy(){
        RPCActionDto copy = new RPCActionDto();
        copy.interfaceId = interfaceId;
        copy.clientInfo = clientInfo;
        copy.clientVariable = clientVariable;
        copy.actionName = actionName;
        if (responseParam != null)
            copy.responseParam = responseParam.copy();
        if (requestParams != null)
            copy.requestParams = requestParams.stream().map(ParamDto::copy).collect(Collectors.toList());
        copy.responseVariable = responseVariable;
        copy.controllerVariable = controllerVariable;
        copy.doGenerateAssertions = doGenerateAssertions;
        copy.doGenerateTestScript = doGenerateTestScript;
        copy.maxAssertionForDataInCollection = maxAssertionForDataInCollection;
        copy.isAuthorized = isAuthorized;
        if (mockRPCExternalServiceDtos != null)
            copy.mockRPCExternalServiceDtos = mockRPCExternalServiceDtos.stream().map(MockRPCExternalServiceDto::copy).collect(Collectors.toList());
        if (mockDatabaseDtos != null)
            copy.mockDatabaseDtos = mockDatabaseDtos.stream().map(MockDatabaseDto::copy).collect(Collectors.toList());
        if (missingDto != null)
            copy.missingDto = new ArrayList<>(missingDto);
        return copy;
    }

    /**
     *
     * @return a complete copy of RPCActionDto including its schema info,
     * eg, possible auth candidates and pe-defined values in requests
     */
    public RPCActionDto copyComplete(){
        RPCActionDto copy = copy();
        if (copy.requiredAuthCandidates != null)
            copy.requiredAuthCandidates = new ArrayList<>(requiredAuthCandidates);
        if (copy.relatedCustomization != null)
            copy.relatedCustomization = new HashSet<>(relatedCustomization);
        return copy;
    }

    /**
     *
     * @return descriptive info for the action, ie, interface::actionName
     */
    public String descriptiveInfo(){
        return ((interfaceId!=null)?interfaceId:"NULL_INTERFACE")+
                "::"+((actionName!=null)?actionName:"NULL_ACTION_NAME")+
                "("+ requestParams.stream().map(s-> s.type.type.toString()).collect(Collectors.joining(",")) +")";
    }

    /**
     *
     * @return if the action needs mock object
     */
    public boolean mockObjectNeeded(){
        return (mockDatabaseDtos != null && (!mockDatabaseDtos.isEmpty())) || (mockRPCExternalServiceDtos!=null && (!mockRPCExternalServiceDtos.isEmpty()));
    }

}
