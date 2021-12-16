package org.evomaster.client.java.controller;


import org.evomaster.client.java.controller.api.dto.CustomizedCallResultCode;

public interface CustomizedResponseHandler {


    // CustomizedCallResultCode categorizeBasedOnValue(Object value);


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
}
