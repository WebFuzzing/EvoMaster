package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Mapping of external service hostname to internal WireMock
     * instance addresses.
     */
    public Map<String, String> externalServiceMapping = new HashMap<>();
}
