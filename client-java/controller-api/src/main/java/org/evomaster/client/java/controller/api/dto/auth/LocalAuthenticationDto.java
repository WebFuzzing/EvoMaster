package org.evomaster.client.java.controller.api.dto.auth;


public class LocalAuthenticationDto {

    /**
     * specify value of input param of handleLocalAuthenticationSetup
     */
    public String authenticationInfo;

    /**
     * specify the auth if it is only applicable to specified endpoints with corresponding annotation
     * Note that it is nullable indicating that the auth could be applied for all endpoints (eg, global auth)
     */
    public String annotationOnEndpoint;
}
