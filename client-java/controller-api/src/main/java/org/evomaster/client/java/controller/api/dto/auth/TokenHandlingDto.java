package org.evomaster.client.java.controller.api.dto.auth;

public class TokenHandlingDto {

    /**
     *  How to extract the token from a JSON response, as such
     *  JSON could have few fields, possibly nested.
     *  It is expressed as a JSON Pointer
     */
    public String extractFromField;


    /**
     * When sending a token in an HTTP header, specify to which header to add it (e.g., "Authorization")
     */
    public String httpHeaderName;

    /**
     * When sending out the obtained token in an HTTP header,
     * specify if there should be any prefix (e.g., "Bearer " or "JWT ").
     * If needed, make sure it has trailing space(s).
     */
    public String headerPrefix;
}
