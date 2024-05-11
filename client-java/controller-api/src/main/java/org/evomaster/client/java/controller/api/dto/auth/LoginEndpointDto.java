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
     * The raw payload to send, as a string
     */
    public String payloadRaw;

    /**
     * Payload with username and password information.
     * It will be automatically formatted in a proper payload based on content type.
     */
    public PayloadUsernamePasswordDto payloadUserPwd;

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
     * Specify how to extract token from response, and how to use it for auth in following requests.
     * Not needed if rather expect to get back a cookie.
     */
    public TokenHandlingDto token;

    /**
     * Specify if we are expecting to get cookies from the login endpoint.
     * If so, we use those as auth info in following requests, instead of trying to extract
     * an auth token from the response payload.
     */
    public Boolean expectCookies;
}
