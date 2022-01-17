package org.evomaster.client.java.controller.api.dto;


import java.util.List;

/**
 * dto is used to specify multiple seeds for a field.
 * the field could be regarded as independent @see {@link CustomizedRequestValueDto#keyValues)}
 */
public class keyValuesDto {

    /**
     * an independent key
     */
    public String key;


    /**
     * candidate values of the key
     */
    public List<String> values;

}
