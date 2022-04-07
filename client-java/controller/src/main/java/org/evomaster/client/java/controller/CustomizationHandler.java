package org.evomaster.client.java.controller;


import org.evomaster.client.java.controller.api.dto.CustomizedCallResultCode;
import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;
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
}
