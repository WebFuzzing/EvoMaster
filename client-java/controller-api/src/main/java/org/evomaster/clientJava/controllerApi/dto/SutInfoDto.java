package org.evomaster.clientJava.controllerApi.dto;

import org.evomaster.clientJava.controllerApi.dto.database.schema.DbSchemaDto;

import java.util.List;

public class SutInfoDto {

    /**
     * The full URL of where the Swagger JSON data can be located
     */
    public String swaggerJsonUrl;


    /**
     * Whether the SUT is running or not
     */
    public Boolean isSutRunning;

    /**
     * The base URL of the running SUT (if any).
     * E.g., "http://localhost:8080"
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
     * When testing a REST API, there might be some endpoints that are not
     * so important to test.
     * For example, in Spring, health-check endpoints like "/heapdump"
     * are not so interesting to test, and they can be very expensive to run.
     */
    public List<String> endpointsToSkip;


    /**
     * If the application is using a SQL database, then we need to
     * know its schema to be able to do operations on it.
     */
    public DbSchemaDto sqlSchemaDto;
}
