package org.evomaster.client.java.instrumentation.heuristic.validator.javax;

import javax.validation.constraints.Pattern;

public class PatternBean {


    @Pattern(regexp = "foo")
    public String foo;
}
