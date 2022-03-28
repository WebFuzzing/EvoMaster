package org.evomaster.client.java.controller.api.dto;

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
     * The headers needed for authentication
     */
    public List<HeaderDto> headers = new ArrayList<>();

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
