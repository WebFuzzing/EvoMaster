package org.evomaster.client.java.instrumentation.heuristic.validator;

import org.evomaster.client.java.instrumentation.heuristic.validator.custom.CustomFieldConstraint;

public class CustomBean {

    @CustomFieldConstraint
    public String foo;

    @CustomFieldConstraint
    public String bar;
}
