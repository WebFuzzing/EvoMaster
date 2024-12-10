package org.evomaster.client.java.controller.api.dto.auth;

/**
 * A HTTP header, which is a key=value pair
 */
public class HeaderDto {

    /**
     * The header name
     */
    public String name;

    /**
     * The value of the header
     */
    public String value;


    public HeaderDto() {
    }

    public HeaderDto(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
