package org.evomaster.solver.smtlib.assertion;

import java.util.List;
import java.util.stream.Collectors;

public class OrAssertion extends Assertion {
    private final List<Assertion> assertions;

    public OrAssertion(List<Assertion> assertions) {
        if (assertions.size() < 2) {
            throw new IllegalArgumentException("And must have at least two assertions");
        }
        this.assertions = assertions;
    }

    @Override
    public String toString() {
        return "(or " + assertions.stream().map(Object::toString).collect(Collectors.joining(" ")) + ")";
    }
}
