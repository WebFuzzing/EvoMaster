package org.evomaster.client.java.controller;


import org.evomaster.client.java.controller.api.dto.ActionResponseDto;
import org.evomaster.client.java.controller.api.dto.CustomizedCallResultCode;
import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.EvaluatedRPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.MockRPCExternalServiceDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.problem.rpc.CustomizedNotNullAnnotationForRPCDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCTestDto;

import java.util.List;

/**
 * contains a set of method to customize info for testing
 *
 * need to check with Andrea if putting them in this interface or moving them to SutController
 */
public interface CustomizationHandler {


    /**
     * <p>
     * categorize result based on response
     * </p>
     *
     * @param response is a given response
     * @return a call result code based on the response
     *
     */
    CustomizedCallResultCode categorizeBasedOnResponse(Object response);

    /**
     * <p>
     * specify candidate values in requests
     * </p>
     *
     *
     * @return a list of specified values for requests
     *
     */
    List<CustomizedRequestValueDto> getCustomizedValueInRequests();

    /**
     * <p>
     *     specify customized annotation indicating that field in DTO is not null if any
     * </p>
     *
     * @return a list of such annotation
     */
    List<CustomizedNotNullAnnotationForRPCDto> specifyCustomizedNotNullAnnotation();

    /**
     * <p>
     *     specify seeded tests for RPC
     * </p>
     *
     * @return a list of dto for seeded tests
     */
    List<SeededRPCTestDto> seedRPCTests();


    /**
     * <p>
     *     here we provide additional handling on the generated RPC tests
     * </p>
     * @param externalServiceDtos represent info is to mock responses of external services
     * @param sqlInsertions represent a sequence of SQL insertions
     * @param actions represent a list of RPC actions to execute in this test with returned responses
     * @return a result of handling of additional RPC Test
     */
    boolean customizeRPCTestOutput(List<MockRPCExternalServiceDto> externalServiceDtos, List<String> sqlInsertions, List<EvaluatedRPCActionDto> actions);

    /**
     * <p>
     *     implement how to enable/disable mocking of RPC based external services
     * </p>
     * @param externalServiceDtos contains info about how to setup responses
     * @param enabled reflect to enable (set it true) or disable (set it false) the specified external service dtos.
     *                Note that null [externalServiceDtos] with false [enabled] means that all existing external service setup should be disabled.
     * @return whether the mocked instance starts successfully,
     */
    boolean customizeMockingRPCExternalService(List<MockRPCExternalServiceDto> externalServiceDtos, boolean enabled);
}
