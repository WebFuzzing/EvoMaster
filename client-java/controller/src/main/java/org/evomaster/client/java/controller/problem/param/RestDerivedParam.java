package org.evomaster.client.java.controller.problem.param;

import java.util.Set;

public class RestDerivedParam {

    /**
     * The name of the parameter
     */
    public final String paramName;

    /**
     * The context in which this parameter is used, eg, whether it is a body payload or a query parameter.
     * This information is needed, as EM will need to determine which other values to use to derive this param.
     */
    public final DerivedParamContext context;

    /**
     * In case the parameter is used differently in different endpoints, specify for which endpoints this
     * derivation applies. If left null, it will apply to all endpoints where this param is present.
     */
    public final Set<String> endpointPaths;

    /**
     * Optional integer specifying in which order the updates are done.
     * If all updates are independent (or there is only 1), then there is no point in specifying this value.
     * However, if the derivation of A depends on first deriving B, then A should get an higher order than B,
     * eg 1 vs 0.
     * In this case, first B is computed based on current state, and then, A is computed with current state
     * updated with derived B.
     */
    public final Integer order;

    public RestDerivedParam(
            String paramName,
            DerivedParamContext context,
            Set<String> endpointPaths,
            Integer order
    ) {
        this.paramName = paramName;
        this.context = context;
        this.endpointPaths = endpointPaths;
        this.order = order;
    }
}
