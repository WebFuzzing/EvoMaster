package org.evomaster.clientJava.controllerApi.dto;

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
}
