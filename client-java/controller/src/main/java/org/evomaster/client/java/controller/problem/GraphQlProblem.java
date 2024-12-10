package org.evomaster.client.java.controller.problem;

import java.util.List;

public class GraphQlProblem extends ProblemInfo{

    /**
     * The endpoint path (not the full URL) in the SUT that expects incoming GraphQL queries.
     * Most of the time, this will just be "/graphql"
     */
    private final String endpoint;

    public GraphQlProblem() {
        this("/graphql");
    }

    public GraphQlProblem(String endpoint) {
        this.endpoint = endpoint;
    }


    public String getEndpoint() {
        return endpoint;
    }


    @Override
    public GraphQlProblem withServicesToNotMock(List<ExternalService> servicesToNotMock){
        GraphQlProblem p =  new GraphQlProblem(this.endpoint);
        p.servicesToNotMock.addAll(servicesToNotMock);
        return p;
    }
}
