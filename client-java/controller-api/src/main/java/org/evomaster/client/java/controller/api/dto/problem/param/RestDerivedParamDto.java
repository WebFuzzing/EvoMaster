package org.evomaster.client.java.controller.api.dto.problem.param;

import java.util.Set;

public class RestDerivedParamDto {

    /**
     * The name of the parameter
     */
    public  String paramName;

    /**
     * The context in which this parameter is used, eg, whether it is a body payload or a query parameter.
     * This information is needed, as EM will need to determine which other values to use to derive this param.
     */
    public String context;

    /**
     * In case the parameter is used differently in different endpoints, specify for which endpoints this
     * derivation applies. If left null, it will apply to all endpoints where this param is present.
     */
    public Set<String> endpointPaths;

}
