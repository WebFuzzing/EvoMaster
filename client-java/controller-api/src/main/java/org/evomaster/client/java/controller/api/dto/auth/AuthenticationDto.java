package org.evomaster.client.java.controller.api.dto.auth;

import java.util.ArrayList;
import java.util.List;

/**
 * To authenticate a user, would need specific settings, like
 * specific values in the HTTP headers (eg, cookies)
 */
public class AuthenticationDto {

    /**
     * The name given to this authentication info.
     * Just needed for display/debugging reasons
     */
    public String name;

    /**
     * The headers needed for authentication.
     * This is used to represent cases in which auth info is static/fixed,
     * eg when passing an id or username/password through a HTTP header (and not
     * using for example a dynamically generated token from a login endpoint first).
     */
    public List<HeaderDto> fixedHeaders = new ArrayList<>();

    /**
     * If the login is based on cookies, need to provide info on
     * how to get such a cookie
     */
    public CookieLoginDto cookieLogin;

    /**
     * If the login is based on tokens, retrieved via JSON messages,
     * specify how to do it
     */
    public JsonTokenPostLoginDto jsonTokenPostLogin;

    /**
     * if the auth is processed based on RPC endpoints,
     * specify what info are required to execute the endpoint
     */
    public JsonAuthRPCEndpointDto jsonAuthEndpoint;

    /**
     * if the auth is processed with handleLocalAuthenticationSetup
     * specify what info is as input to setup auth with handleLocalAuthenticationSetup
     */
    public LocalAuthenticationDto localAuthSetup;


    public AuthenticationDto() {
    }

    public AuthenticationDto(String name) {
        this.name = name;
    }
}
