package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.problem.ExternalServiceDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;

import java.util.*;

/**
 * Created by arcuri82 on 16-Sep-19.
 */
public class ActionDto {

    /**
     * The index of this action in the test.
     * Eg, in a test with 10 indices, the index would be
     * between 0 and 9
     */
    public Integer index = null;

    /**
     * The name of the action, used to identify its type.
     * It is not unique.
     * Eg, for REST, it could be something like VERB:PATH
     */
    public String name = null;

    /**
     * A list (possibly empty) of String values used in the action.
     * This info can be used for different kinds of taint analysis, eg
     * to check how such values are used in the SUT
     */
    public List<String> inputVariables = new ArrayList<>();

    /**
     * info to execute an RPC action.
     * note that this is only used when handling RPC problem
     */
    public RPCActionDto rpcCall;

    /**
     * This list of DTOs represents the external service mappings.
     * Key: WireMock signature which is a string of protocol, hostname, and port.
     * Value: Contains [ExternalServiceMappingDto]
     */
    public Map<String, ExternalServiceMappingDto> externalServiceMapping = new HashMap<>();

    /**
     * Mapping of external service domain name and local address used
     * for mocking.
     */
    public Map<String, String> localAddressMapping = new HashMap<>();

    /**
     * List of skipped external services from handling inside core.
     * Information will be retrieved from [ProblemInfo] provided inside the
     * driver.
     */
    public List<ExternalServiceDto> skippedExternalServices = new ArrayList<>();

}
