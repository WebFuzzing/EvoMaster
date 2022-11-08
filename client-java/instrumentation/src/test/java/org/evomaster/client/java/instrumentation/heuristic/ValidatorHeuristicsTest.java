package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.instrumentation.heuristic.validator.NoConstraintsBean;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorHeuristicsTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testNoConstraintsBean(){

        NoConstraintsBean bean = new NoConstraintsBean();

        assertThrows(IllegalArgumentException.class, () -> ValidatorHeuristics.computeTruthness(validator, bean));
    }
}