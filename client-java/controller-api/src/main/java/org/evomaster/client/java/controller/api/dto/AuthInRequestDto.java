package org.evomaster.client.java.controller.api.dto;

import java.util.List;

public class AuthInRequestDto {

    /**
     * auth fields and corresponding values in request
     */
    public List<AuthKeyValueDto> values;

    /**
     * specify the auth if it is only applicable to specified endpoints with corresponding annotation
     * Note that it is nullable indicating that the auth could be applied for all endpoints (eg, global auth)
     */
    public String annotationOnEndpoint;

}
