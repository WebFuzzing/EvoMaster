package org.evomaster.client.java.controller.problem;

public class GraphQlProblem implements ProblemInfo{

    /**
     * The endpoint in the SUT that expect incoming GraphQL queries
     */
    private final String endpoint;

    public GraphQlProblem(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
