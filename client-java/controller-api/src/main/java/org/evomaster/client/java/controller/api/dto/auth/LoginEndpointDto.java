package org.evomaster.client.java.controller.api.dto.auth;

/**
 * Authentication based on making a request to a login endpoint.
 * A token or cookie is then extracted from the response, and used in following HTTP calls
 */
public class LoginEndpointDto {


    /**
     * The endpoint path (eg "/login") where to execute the login.
     * It assumes it is on same server of API.
     * If not, rather use externalEndpointURL
     */
    public String endpoint;

    /**
     * If the login endpoint is on a different server, here can rather specify the full URL for it.
     */
    public String externalEndpointURL;

    /**
     * The payload to send, as a string
     */
    public String payload;

    /**
     * The verb used to connect to the login endpoint.
     * Most of the time, this will be a POST.
     */
    public HttpVerb verb;

    /**
     * Specify the format in which the payload is sent to the login endpoint.
     * A common example is "application/json"
     */
    public String contentType;

    /**
     *  How to extract the token from a JSON response, as such
     *  JSON could have few fields, possibly nested.
     *  It is expressed as a JSON Pointer
     */
    public String extractTokenFromJSONField;

    /**
     * When sending out the obtained token in a HTTP header,
     * specify if there should be any prefix (e.g., "Bearer " or "JWT ")
     */
    public String headerPrefix;

    /**
     * Specify if we are expecting to get cookies from the login endpoint.
     * If so, we use those as auth info in following requests, instead of trying to extract
     * an auth token from the response payload.
     */
    public Boolean expectCookies;
}
