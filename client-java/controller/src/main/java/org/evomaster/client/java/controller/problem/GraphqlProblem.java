package org.evomaster.client.java.controller.problem;


public class GraphqlProblem implements ProblemInfo{

    public String graphqlEndpointUrl;

    public GraphqlProblem(String graphqlEndpointUrl) {
        /*
        TODO: we should consider skipping some of the operations in graphql
         */
        this.graphqlEndpointUrl = graphqlEndpointUrl;
    }

    public String getGraphqlEndpointUrl() {
        return graphqlEndpointUrl;
    }
}
