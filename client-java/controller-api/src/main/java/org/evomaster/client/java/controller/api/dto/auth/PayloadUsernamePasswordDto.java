package org.evomaster.client.java.controller.api.dto.auth;

public class PayloadUsernamePasswordDto {

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
}
