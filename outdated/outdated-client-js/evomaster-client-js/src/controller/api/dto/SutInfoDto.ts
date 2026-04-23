import AuthenticationDto from "./AuthenticationDto";
import RestProblemDto from "./problem/RestProblemDto";
import UnitsInfoDto from "./UnitsInfoDto";
import GraphQLProblemDto from "./problem/GraphQLProblemDto";

/*
    Note: this enum must be kept in sync with what declared in
    org.evomaster.core.output.OutputFormat
 */
export enum OutputFormat {
    JAVA_JUNIT_5 = "JAVA_JUNIT_5",
    JAVA_JUNIT_4 = "JAVA_JUNIT_4",
    KOTLIN_JUNIT_4 = "KOTLIN_JUNIT_4",
    KOTLIN_JUNIT_5 = "KOTLIN_JUNIT_5",
    JS_JEST = "JS_JEST"
}

export class SutInfoDto {

    /**
     * If the SUT is a RESTful API, here there will be the info
     * on how to interact with it
     */
    public restProblem: RestProblemDto;

    /**
     * If the SUT is a GraphQL API, here there will be the info
     * on how to interact with it
     */
    public graphQLProblem: GraphQLProblemDto;

    /**
     * Whether the SUT is running or not
     */
    public isSutRunning: boolean;

    /**
     * When generating test cases for this SUT, specify the default
     * preferred output format (eg JUnit 4 in Java)
     */
    public defaultOutputFormat: OutputFormat;

    /**
     * The base URL of the running SUT (if any).
     * E.g., "http://localhost:8080"
     * It should only contain the protocol and the hostname/port
     */
    public baseUrlOfSUT: string;

    /**
     * There is no way a testing system can guess passwords, even
     * if given full access to the database storing them (ie, reversing
     * hash values).
     * As such, the SUT might need to provide a set of valid credentials
     */
    public infoForAuthentication: AuthenticationDto[];

    /**
     * If the application is using a SQL database, then we need to
     * know its schema to be able to do operations on it.
     */
    // sqlSchemaDto: DbSchemaDto;

    /**
     * Information about the "units" in the SUT.
     */
    public unitsInfoDto: UnitsInfoDto;
}
