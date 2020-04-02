package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.problem.RestProblemDto;

import java.util.List;

public class SutInfoDto {

    /**
     * If the SUT is a RESTful API, here there will be the info
     * on how to interact with it
     */
    public RestProblemDto restProblem;

    /**
     * Whether the SUT is running or not
     */
    public Boolean isSutRunning;

    /*
        Note: this enum must be kept in sync with what declared in
        org.evomaster.core.output.OutputFormat
     */
    public enum OutputFormat {
        JAVA_JUNIT_5,
        JAVA_JUNIT_4,
        KOTLIN_JUNIT_4,
        KOTLIN_JUNIT_5,
        JS_JEST
    }

    /**
     * When generating test cases for this SUT, specify the default
     * preferred output format (eg JUnit 4 in Java)
     */
    public OutputFormat defaultOutputFormat;

    /**
     * The base URL of the running SUT (if any).
     * E.g., "http://localhost:8080"
     * It should only contain the protocol and the hostname/port
     */
    public String baseUrlOfSUT;

    /**
     * There is no way a testing system can guess passwords, even
     * if given full access to the database storing them (ie, reversing
     * hash values).
     * As such, the SUT might need to provide a set of valid credentials
     */
    public List<AuthenticationDto> infoForAuthentication;


    /**
     * If the application is using a SQL database, then we need to
     * know its schema to be able to do operations on it.
     */
    public DbSchemaDto sqlSchemaDto;


    /**
     * Information about the "units" in the SUT.
     */
    public UnitsInfoDto unitsInfoDto;
}
