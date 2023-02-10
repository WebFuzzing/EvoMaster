package org.evomaster.client.java.instrumentation.heuristic.validator.javax;

import org.evomaster.client.java.instrumentation.heuristic.validator.javax.custom.CustomFieldConstraint;

public class CustomBean {

    @CustomFieldConstraint
    public String foo;

    @CustomFieldConstraint
    public String bar;
}
