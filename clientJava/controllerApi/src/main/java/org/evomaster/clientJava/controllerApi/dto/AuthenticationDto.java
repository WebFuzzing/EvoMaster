package org.evomaster.clientJava.controllerApi.dto;

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


    public AuthenticationDto() {
    }

    public AuthenticationDto(String name) {
        this.name = name;
    }
}
