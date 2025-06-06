package org.evomaster.client.java.controller.api.dto.auth;

import com.webfuzzing.commons.auth.AuthenticationInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * To authenticate a user, would need specific settings, like
 * specific values in the HTTP headers (eg, cookies)
 */
public class AuthenticationDto extends AuthenticationInfo {

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
        this.setName(name);
    }
}
