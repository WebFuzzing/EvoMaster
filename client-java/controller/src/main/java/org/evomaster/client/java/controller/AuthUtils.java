package org.evomaster.client.java.controller;

import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.CookieLoginDto;
import org.evomaster.client.java.controller.api.dto.HeaderDto;

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
        dto.headers.add(new HeaderDto("Authorization", authorizationValue));

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

        CookieLoginDto cookie = new CookieLoginDto();
        cookie.httpVerb = CookieLoginDto.HttpVerb.POST;
        cookie.contentType = CookieLoginDto.ContentType.X_WWW_FORM_URLENCODED;
        cookie.usernameField = "username";
        cookie.passwordField = "password";
        cookie.loginEndpointUrl = "/login";
        cookie.username = username;
        cookie.password = password;

        AuthenticationDto dto = new AuthenticationDto(dtoName);
        dto.cookieLogin = cookie;

        return dto;
    }
}
