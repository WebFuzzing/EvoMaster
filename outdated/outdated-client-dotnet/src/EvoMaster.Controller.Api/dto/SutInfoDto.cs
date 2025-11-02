using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class SutInfoDto {
        /**
     * If the SUT is a RESTful API, here there will be the info
     * on how to interact with it
     */
        public RestProblemDto RestProblem { get; set; }

        /**
     * Whether the SUT is running or not
     */
        public bool? IsSutRunning { get; set; }

        /**
     * When generating test cases for this SUT, specify the default
     * preferred output format (eg JUnit 4 in Java)
     */
        public OutputFormat DefaultOutputFormat { get; set; }

        /**
     * The base URL of the running SUT (if any).
     * E.g., "http://localhost:8080"
     * It should only contain the protocol and the hostname/port
     */
        public string BaseUrlOfSUT { get; set; }

        /**
     * There is no way a testing system can guess passwords, even
     * if given full access to the database storing them (ie, reversing
     * hash values).
     * As such, the SUT might need to provide a set of valid credentials
     */
        public IList<AuthenticationDto> InfoForAuthentication { get; set; } //TODO: Why no default value?

        /**
     * If the application is using a SQL database, then we need to
     * know its schema to be able to do operations on it.
     */
        public DbSchemaDto SqlSchemaDto { get; set; }

        /**
     * Information about the "units" in the SUT.
     */
        public UnitsInfoDto UnitsInfoDto { get; set; }
    }

    //TODO: Review possible values for this enum
    /*
      Note: this enum must be kept in sync with what declared in
      org.evomaster.core.output.OutputFormat
   */
    public enum OutputFormat {
        JAVA_JUNIT_5,
        JAVA_JUNIT_4,
        KOTLIN_JUNIT_4,
        KOTLIN_JUNIT_5,
        JS_JEST,
        CSHARP_XUNIT
    }
}