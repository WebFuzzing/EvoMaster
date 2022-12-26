package org.evomaster.client.java.instrumentation.heuristic.validator;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class ConflictBean {

    @NotNull
    @Positive
    public Integer x;
}
