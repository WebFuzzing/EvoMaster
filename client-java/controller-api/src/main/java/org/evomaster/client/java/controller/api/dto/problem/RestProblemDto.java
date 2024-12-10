package org.evomaster.client.java.controller.api.dto.problem;

import java.util.List;

/**
 * Created by arcuri82 on 05-Nov-18.
 */
public class RestProblemDto extends ProblemInfoDto{

    /**
     * The full URL of where the Open/API schema can be located.
     */
    public String openApiUrl;

    /**
     * When testing a REST API, there might be some endpoints that are not
     * so important to test.
     * For example, in Spring, health-check endpoints like "/heapdump"
     * are not so interesting to test, and they can be very expensive to run.
     */
    public List<String> endpointsToSkip;

    /**
     * Full schema of the OpenAPI, as a text. If this is specified, then openApiUrl
     * should not be set
     */
    public String openApiSchema;
}
