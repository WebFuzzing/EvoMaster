package org.evomaster.client.java.controller.api.dto;

/**
 * dto is used to specify a seed for a field
 * which is typically used for specifying combined fields [CustomizedRequestValueDto#combinedKeyValuePairs]
 * eg, ID and corresponding passcode
 */
public class KeyValuePairDto {

    /**
     * field name which is related to auth in request
     */
    public String fieldKey;

    /**
     * value of field for the key
     */
    public String fieldValue;
}
