package org.evomaster.client.java.controller;

import com.webfuzzing.commons.auth.Header;
import com.webfuzzing.commons.auth.LoginEndpoint;
import com.webfuzzing.commons.auth.TokenHandling;
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
        Header header = new Header();
        header.setName("Authorization");
        header.setValue(authorizationValue);
        dto.getFixedHeaders().add(header);

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
        return getForDefaultSpringFormLogin(dtoName, username, password, "/login");
    }


    /**
     * DTO representing the use of authentication via a X-WWW-FORM-URLENCODED POST submission.
     * Assuming default names used in SpringSecurity for default formLogin() configuration.
     *
     * When using this kind of DTO, EM will first do a POST on such endpoint with valid credentials,
     * and then use the resulting cookie for the following HTTP requests.
     *
     * @param dtoName a name used to identify this dto. Mainly needed for debugging
     * @param username    the id of a user
     * @param password  password for that user
     * @param endpoint  the url of the endpoint to use for the login
     * @return a DTO
     */
    public static AuthenticationDto getForDefaultSpringFormLogin(String dtoName, String username, String password, String endpoint){

        LoginEndpoint cookie = new LoginEndpoint();

        cookie.setEndpoint(endpoint);
        cookie.setVerb(LoginEndpoint.HttpVerb.POST);
        cookie.setContentType("application/x-www-form-urlencoded");
        cookie.setExpectCookies(true);
        try {
            String payload;
            String usernameField = URLEncoder.encode("username", "UTF-8");
            String passwordField = URLEncoder.encode("password", "UTF-8");
            payload = usernameField + "=" + URLEncoder.encode(username, "UTF-8");
            payload += "&";
            payload += passwordField + "="+ URLEncoder.encode(password, "UTF-8");
            cookie.setPayloadRaw(payload);
        }catch (UnsupportedEncodingException e){
            throw new RuntimeException(e); //ah, the joys of Java...
        }
        AuthenticationDto dto = new AuthenticationDto(dtoName);
        dto.setLoginEndpointAuth(cookie);

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
        return getForJsonToken(dtoName, postEndpoint, payload, extractFromField, headerPrefix,"application/json");
    }

    /**
     * Creates an AuthenticationDto object configured to obtain a JSON token from a login endpoint.
     * The postEndpoint parameter can be interpreted in two different ways:
     * <ul>
     *   <li>If postEndpoint starts with "http://" or "https://", it is treated as a full external URL</li>
     *   <li>Otherwise, it is treated as a relative path that will be combined with a base URL</li>
     * </ul>
     * @param dtoName The name to assign to the AuthenticationDto
     * @param postEndpoint The endpoint URL or path (see description above for interpretation)
     * @param payload The request payload to send to the login endpoint
     * @param extractFromField The field in the response that contains the token
     * @param headerPrefix The prefix to add to the token (e.g., "Bearer ")
     * @param contentType The content type to use for the request
     * @return Configured AuthenticationDto object with login endpoint settings
     */
    public static AuthenticationDto getForJsonToken(
            String dtoName,
            String postEndpoint,
            String payload,
            String extractFromField,
            String headerPrefix,
            String contentType
    ){

        LoginEndpoint le = new LoginEndpoint();

        if(postEndpoint.startsWith("http://") || postEndpoint.startsWith("https://")){
            le.setExternalEndpointURL(postEndpoint);
        } else {
            le.setEndpoint(postEndpoint);
        }

        le.setVerb(LoginEndpoint.HttpVerb.POST);
        le.setContentType(contentType);
        le.setExpectCookies(false);
        le.setPayloadRaw(payload);
        le.setToken(new TokenHandling());
        le.getToken().setExtractFromField(extractFromField);
        le.getToken().setHeaderPrefix(headerPrefix);
        le.getToken().setHttpHeaderName("Authorization");

        AuthenticationDto dto = new AuthenticationDto(dtoName);
        dto.setLoginEndpointAuth(le);

        return dto;
    }
}
