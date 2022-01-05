package org.evomaster.client.java.controller.api.dto;

import java.util.List;

public class CustomizedRequestValueDto {

    /**
     * a set of fields and corresponding values in request
     */
    public List<KeyValueDto> combinedKeyValuePairs;

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
     * specify the value which is only applicable to the specified endpoint
     * Note that it is nullable indicating that the value could be applied for endpoint with any name
     */
    public String specificEndpointName;

    /**
     * specify the value if it is only applicable to input with the specific type
     * Note that it is nullable indicating that the value could be applied for all types of input params
     * which contains field or param with name as key
     */
    public String specificRequestTypeName;
}
