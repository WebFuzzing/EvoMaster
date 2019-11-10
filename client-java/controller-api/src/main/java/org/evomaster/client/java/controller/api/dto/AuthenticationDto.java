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

    public AuthenticationDto() {
    }

    public AuthenticationDto(String name) {
        this.name = name;
    }
}
