package org.evomaster.client.java.controller.problem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arcuri82 on 05-Nov-18.
 */
public class RestProblem implements ProblemInfo{

    private final String swaggerJsonUrl;

    private final List<String> endpointsToSkip;

    /**
     *
     * @param swaggerJsonUrl Provide the URL of where the swagger.json can be found
     * @param endpointsToSkip When testing a REST API, there might be some endpoints that are not
     *       so important to test.
     *       For example, in Spring, health-check endpoints like "/heapdump"
     *       are not so interesting to test, and they can be very expensive to run.
     *       Here can specify a list of endpoints (as defined in the schema) to skip.
     */
    public RestProblem(String swaggerJsonUrl, List<String> endpointsToSkip) {
        this.swaggerJsonUrl = swaggerJsonUrl;
        this.endpointsToSkip = endpointsToSkip == null
                ? new ArrayList<>()
                : new ArrayList<>(endpointsToSkip);
    }

    public String getSwaggerJsonUrl() {
        return swaggerJsonUrl;
    }

    public List<String> getEndpointsToSkip() {
        return endpointsToSkip;
    }
}
