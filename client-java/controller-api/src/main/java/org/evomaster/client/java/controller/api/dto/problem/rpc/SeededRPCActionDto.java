package org.evomaster.client.java.controller.api.dto.problem.rpc;

import org.evomaster.client.java.controller.api.dto.MockDatabaseDto;

import java.util.List;

/**
 * seeded RPC action
 */
public class SeededRPCActionDto {

    /**
     * full name of the interface name
     */
    public String interfaceName;

    /**
     * RPC function name
     */
    public String functionName;

    /**
     * a list of info to set up responses of external services with mocking if exists
     */
    public List<MockRPCExternalServiceDto> mockRPCExternalServiceDtos;

    /**
     * a list of info to set up mock objects for databases
     */
    public List<MockDatabaseDto> mockDatabaseDtos;

    /**
     * input parameters with json format
     * note the length of [inputParamTypes] must be same as [inputParams]
     */
    public List<String> inputParams;

    /**
     * type of input parameters,
     * note the length of [inputParamTypes] must be same as [inputParams]
     */
    public List<String> inputParamTypes;

    /**
     * expected response if has
     *
     * currently, we only support json format
     */
    public String expectedResponse;

    /**
     *
     * @return descriptive info for the action, ie, interface::actionName
     */
    public String descriptiveInfo(){
        return ((interfaceName!=null)?interfaceName:"SEED_TEST_NULL_INTERFACE")+
                "::"+((functionName!=null)?functionName:"SEED_TEST_NULL_ACTION_NAME");
    }

}
