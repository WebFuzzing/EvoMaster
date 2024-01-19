package org.evomaster.client.java.controller.problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by arcuri82 on 05-Nov-18.
 */
public class RestProblem extends ProblemInfo{

    private final String openApiUrl;

    private final List<String> endpointsToSkip;

    private final String openApiSchema;

    public RestProblem(String openApiUrl, List<String> endpointsToSkip) {
        this(openApiUrl, endpointsToSkip, null);
    }


    /**
     *
     * @param openApiUrl Provide the URL of where the OpenAPI schema can be found.
     * @param endpointsToSkip When testing a REST API, there might be some endpoints that are not
     *       so important to test.
     *       For example, in Spring, health-check endpoints like "/heapdump"
     *       are not so interesting to test, and they can be very expensive to run.
     *       Here can specify a list of endpoints (as defined in the schema) to skip.
     * @param openApiSchema the actual schema, as a string. Note, if this specified, then
     *                       openApiUrl must be null
     */
    public RestProblem(String openApiUrl, List<String> endpointsToSkip, String openApiSchema) {
        this.openApiUrl = openApiUrl;
        this.endpointsToSkip = endpointsToSkip == null
                ? new ArrayList<>()
                : new ArrayList<>(endpointsToSkip);
        this.openApiSchema = openApiSchema;

        boolean url = openApiUrl != null && !openApiUrl.isEmpty();
        boolean schema = openApiSchema != null && !openApiSchema.isEmpty();

        if(!url && !schema){
            throw new IllegalArgumentException("MUST either provide a URL or a full schema for OpenAPI");
        }
        if(url && schema){
            throw new IllegalArgumentException("Cannot specify BOTH a URL and a whole schema. Choose one only");
        }
    }

    public String getOpenApiUrl() {
        return openApiUrl;
    }

    public List<String> getEndpointsToSkip() {
        return Collections.unmodifiableList(endpointsToSkip);
    }

    public String getOpenApiSchema() {
        return openApiSchema;
    }

    @Override
    public RestProblem withServicesToNotMock(List<ExternalService> servicesToNotMock){
        RestProblem p =  new RestProblem(this.openApiUrl, this.endpointsToSkip, this.openApiSchema);
        p.servicesToNotMock.addAll(servicesToNotMock);
        return p;
    }
}
