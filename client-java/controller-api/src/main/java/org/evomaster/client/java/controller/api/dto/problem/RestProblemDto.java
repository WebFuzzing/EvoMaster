package org.evomaster.client.java.controller.api.dto.problem;

import java.util.List;

/**
 * Created by arcuri82 on 05-Nov-18.
 */
public class RestProblemDto {

    /**
     * The full URL of where the Swagger JSON data can be located
     */
    public String swaggerJsonUrl;

    /**
     * When testing a REST API, there might be some endpoints that are not
     * so important to test.
     * For example, in Spring, health-check endpoints like "/heapdump"
     * are not so interesting to test, and they can be very expensive to run.
     */
    public List<String> endpointsToSkip;
}
