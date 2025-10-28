import ProblemInfo from "./ProblemInfo";

export default class RestProblemDto implements ProblemInfo {

    /**
     * The full URL of where the OpenAPI schema can be located
     */
    public openApiUrl: string;

    /**
     * When testing a REST API, there might be some endpoints that are not
     * so important to test.
     * For example, in Spring, health-check endpoints like "/heapdump"
     * are not so interesting to test, and they can be very expensive to run.
     */
    public endpointsToSkip: string[];
}
