package org.evomaster.client.java.controller.api.dto;

import java.util.List;

public class CustomizedRequestValueDto {

    /**
     * a set of fields and corresponding values in request
     */
    public List<KeyValueDto> keyValuePairs;

    /**
     * a key with a set of candidate values
     */
    public keyValuesDto keyValues;

    /**
     * specify the value if it is only applicable to specified endpoints with corresponding annotation
     * Note that it is nullable indicating that the value could be applied for all endpoints
     */
    public String annotationOnEndpoint;

    /**
     * specify the value if it is only applicable to specified input with the name
     * Note that it is nullable indicating that the value could be applied for all input params
     */
//    public String specificInputParamName;
}
