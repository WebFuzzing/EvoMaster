package org.evomaster.client.java.instrumentation.heuristic.validator;

import javax.validation.constraints.Min;


public class SingleConstraintBean {

    @Min(1)
    public int x;

}
