package org.evomaster.client.java.controller.api.dto;

/**
 * If login is based on a POST endpoint with input a JSON payload, and return
 * a token inside a JSON object, to be used in Authorization header in following
 * HTTP requests
 */
public class JsonTokenPostLoginDto {

    /**
     * The id representing this user that is going to login
     */
    public String userId;

    /**
     * The endpoint where to execute the login
     */
    public String endpoint;

    /**
     * The payload to send, as stringified JSON object
     */
    public String jsonPayload;


    /**
     *  How to extract the token from a JSON response, as such
     *  JSON could have few fields, possibly nested.
     *  It is expressed as a JSON Pointer
     */
    public String extractTokenField;

    /**
     * When sending out the obtained token in the Authorization header,
     * specify if there should be any prefix (e.g., "Bearer " or "JWT ")
     */
    public String headerPrefix;
}
