package org.evomaster.client.java.controller.api.dto;

import java.util.*;

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

    /**
     * Information for taint analysis.
     * When some string inputs are recognized of a specific type (eg,
     * they are used as integers or dates), we keep track of it.
     * The key in this map is the value of the tainted input.
     * The associated list is its possible specializations (which usually
     * will be at most 1).
     */
    public Map<String, List<StringSpecializationInfoDto>> stringSpecializations = new LinkedHashMap<>();


    /**
     * Keep track of the last executed statement done in the SUT.
     * But not in the third-party libraries, just the business logic of the SUT.
     * The statement is represented with a descriptive unique id, like the class name and line number.
     */
    public String lastExecutedStatement = null;


    /**
     * Check if the business logic of the SUT (and not a third-party library) is
     * accessing the raw bytes of HTTP body payload (if any) directly
     */
    public Boolean rawAccessOfHttpBodyPayload = null;


    /**
     * The name of all DTO that have been parsed (eg, with GSON and Jackson).
     * Note: the actual content of schema is queried separately.
     */
    public Set<String> parsedDtoNames = new HashSet<>();


    /**
     * To keep track of all external service calls used under SUT.
     */
    public List<ExternalServiceInfoDto> externalServices = new ArrayList<>();

    /**
     * a list of external service info which is direct to default WM
     */
    public List<ExternalServiceInfoDto> employedDefaultWM = new ArrayList<>();

    public List<HostnameResolutionInfoDto> hostnameResolutionInfoDtos = new ArrayList<>();
}
