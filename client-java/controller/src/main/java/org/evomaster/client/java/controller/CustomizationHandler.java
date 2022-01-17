package org.evomaster.client.java.controller;


import org.evomaster.client.java.controller.api.dto.CustomizedCallResultCode;
import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;

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
}
