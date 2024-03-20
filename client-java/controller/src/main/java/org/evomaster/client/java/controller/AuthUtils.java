package org.evomaster.client.java.controller;

import org.evomaster.client.java.controller.api.dto.auth.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class AuthUtils {


    public static String encode64(String value){

        Objects.requireNonNull(value);

        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        byte[] encoded = Base64.getEncoder().encode(data);

        return new String(encoded);
    }


    /**
     * DTO representing the use of authentication via HTTP Basic (RFC-7617)
     * @param dtoName a name used to identify this dto. Mainly needed for debugging
     * @param userId    the id of a user
     * @param password  password for that user
     * @return a DTO
     */
    public static AuthenticationDto getForBasic(String dtoName, String userId, String password){

        Objects.requireNonNull(userId, password);

        String encoded = encode64(userId + ":" + password);
        String headerValue = "Basic " + encoded;

       return getForAuthorizationHeader(dtoName, headerValue);
    }

    /**
     * DTO representing the use of authentication via the "Authorization" header
     * @param dtoName a name used to identify this dto. Mainly needed for debugging
     * @param authorizationValue    the content of  the "Authorization" header
     * @return a DTO
     */
    public static AuthenticationDto getForAuthorizationHeader(String dtoName, String authorizationValue){

        AuthenticationDto dto = new AuthenticationDto(dtoName);
        dto.fixedHeaders.add(new HeaderDto("Authorization", authorizationValue));

        return dto;
    }


    /**
     * DTO representing the use of authentication via a X-WWW-FORM-URLENCODED POST submission.
     * Assuming default names and endpoint used in SpringSecurity for default formLogin() configuration.
     *
     * When using this kind of DTO, EM will first do a POST on such endpoint with valid credentials,
     * and then use the resulting cookie for the following HTTP requests.
     *
     * @param dtoName a name used to identify this dto. Mainly needed for debugging
     * @param username    the id of a user
     * @param password  password for that user
     * @return a DTO
     */
    public static AuthenticationDto getForDefaultSpringFormLogin(String dtoName, String username, String password){

        LoginEndpointDto cookie = new LoginEndpointDto();

        cookie.endpoint = "/login";
        cookie.verb = HttpVerb.POST;
        cookie.contentType = "application/x-www-form-urlencoded";
        cookie.expectCookies = true;
        try {
            String payload;
            String usernameField = URLEncoder.encode("username", "UTF-8");
            String passwordField = URLEncoder.encode("password", "UTF-8");
            payload = usernameField + "=" + URLEncoder.encode(username, "UTF-8");
            payload += "&";
            payload += passwordField + "="+ URLEncoder.encode(password, "UTF-8");
            cookie.payloadRaw = payload;
        }catch (UnsupportedEncodingException e){
            throw new RuntimeException(e); //ah, the joys of Java...
        }
        AuthenticationDto dto = new AuthenticationDto(dtoName);
        dto.loginEndpointAuth = cookie;

        return dto;
    }


    public static AuthenticationDto getForJWT(
            String dtoName,
            String postEndpoint,
            String payload,
            String extractFromField){

        return  getForJsonToken(dtoName, postEndpoint, payload, extractFromField, "JWT ");
    }


    public static AuthenticationDto getForJsonTokenBearer(
            String dtoName,
            String postEndpoint,
            String payload,
            String extractFromField){

        return  getForJsonToken(dtoName, postEndpoint, payload, extractFromField, "Bearer ");
    }

    public static AuthenticationDto getForJsonToken(
            String dtoName,
            String postEndpoint,
            String payload,
            String extractFromField,
            String headerPrefix
    ){

        LoginEndpointDto le = new LoginEndpointDto();

        le.endpoint = postEndpoint;
        le.verb = HttpVerb.POST;
        le.contentType = "application/json";
        le.expectCookies = false;
        le.payloadRaw = payload;
        le.token = new TokenHandlingDto();
        le.token.extractFromField = extractFromField;
        le.token.headerPrefix = headerPrefix;
        le.token.httpHeaderName = "Authorization";

        AuthenticationDto dto = new AuthenticationDto(dtoName);
        dto.loginEndpointAuth = le;

        return dto;
    }
}
