package org.evomaster.clientJava.controllerApi.dto;

import java.util.HashSet;
import java.util.Set;

public class AdditionalInfoDto {

    /**
     * In REST APIs, it can happen that some query parameters do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    public Set<String> queryParameters = new HashSet<>();


    /**
     * In REST APIs, it can happen that some HTTP headers do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    public Set<String> headers = new HashSet<>();

}
