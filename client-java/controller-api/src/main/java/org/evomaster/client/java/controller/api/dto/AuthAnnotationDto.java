package org.evomaster.client.java.controller.api.dto;

import java.util.List;

public class AuthAnnotationDto {

    /**
     * auth fields and corresponding values in request
     */
    public List<AuthKeyValueDto> values;

    /**
     * name of annotation
     */
    public String annotationName;

}
