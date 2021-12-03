package org.evomaster.client.java.controller.problem;

public class GraphQlProblem implements ProblemInfo{

    /**
     * The endpoint path (not the full URL) in the SUT that expects incoming GraphQL queries.
     * Most of the times, this will just be "/graphql"
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
}
