package org.evomaster.clientJava.controllerApi;

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
}
