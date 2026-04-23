import ProblemInfo from "./ProblemInfo";


export default class GraphQLProblemDto implements ProblemInfo{

    /**
     * The endpoint in the SUT that expect incoming GraphQL queries
     */
    public endpoint: string;
}