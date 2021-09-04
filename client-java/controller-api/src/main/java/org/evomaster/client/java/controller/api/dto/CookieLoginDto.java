package org.evomaster.client.java.controller.api.dto;

/**
 * Information on how to do a login based on username/password,
 * from which we then get a cookie back
 *
 * Created by arcuri82 on 24-Oct-19.
 */
public class CookieLoginDto {


    /**
     * The id of the user
     */
    public String username;

    /**
     * The password of the user.
     * This must NOT be hashed.
     */
    public String password;

    /**
     * The name of the field in the body payload containing the username
     */
    public String usernameField;

    /**
     * The name of the field in the body payload containing the password
     */
    public String passwordField;

    /**
     * The URL of the endpoint, e.g., "/login"
     */
    public String loginEndpointUrl;


    public enum HttpVerb{
        GET, POST
    }

    /**
     * The HTTP verb used to send the data.
     * Usually a "POST".
     */
    public HttpVerb httpVerb;

    public enum ContentType{
        JSON, X_WWW_FORM_URLENCODED
    }

    /**
     * The encoding type used to specify how the data is sent
     */
    public ContentType contentType;
}


