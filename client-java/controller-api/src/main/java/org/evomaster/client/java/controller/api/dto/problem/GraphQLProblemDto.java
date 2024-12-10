package org.evomaster.client.java.controller.api.dto.problem;

public class GraphQLProblemDto extends ProblemInfoDto{

    /**
     * The endpoint path (not the full URL) in the SUT that expects incoming GraphQL queries.
     * Most of the times, this will just be "/graphql"
     */
    public String endpoint;
}
