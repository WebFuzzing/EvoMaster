package org.evomaster.client.java.controller;


import org.evomaster.client.java.controller.api.dto.CustomizedCallResultCode;
import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;
import org.evomaster.client.java.controller.api.dto.MockDatabaseDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.*;
import org.evomaster.client.java.controller.problem.rpc.CustomizedNotNullAnnotationForRPCDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * <p>
     *     implement how to enable/disable customized mock objects for database
     * </p>
     * @param databaseDtos contains info about how to mock databases based on commandName
     * @param enabled reflect to enable (set it true) or disable (set it false) mock objects for sql command based on commandName
     *                Note that null [databaseDtos] with false [enabled] means that all existing mock objects should be disabled.
     * @return whether the mocked instance starts successfully,
     */
    boolean customizeMockingDatabase(List<MockDatabaseDto> databaseDtos, boolean enabled);

    /**
     * <p>
     *     specify importance levels for exceptions
     *     lower value more important, 0 is the most important exception which needs to be fixed earliest
     * </p>
     * @return a map, key is the class of exception, and value is importance level which must not be less than 0
     *
     */
    Map<Class, Integer> getExceptionImportanceLevels();
}
