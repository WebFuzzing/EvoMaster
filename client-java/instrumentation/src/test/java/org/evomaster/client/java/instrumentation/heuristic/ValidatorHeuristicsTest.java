package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.instrumentation.heuristic.validator.BaseConstraintsBean;
import org.evomaster.client.java.instrumentation.heuristic.validator.NoConstraintsBean;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorHeuristicsTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testNoConstraintsBean(){

        NoConstraintsBean bean = new NoConstraintsBean();

        assertThrows(IllegalArgumentException.class, () -> ValidatorHeuristics.computeTruthness(validator, bean));
    }

    @Test
    public void testBaseConstraintsBean(){

        BaseConstraintsBean bean = new BaseConstraintsBean();

        Set<ConstraintViolation<BaseConstraintsBean>> result =  validator.validate(bean);
        //5 constraints, over 4 fields, 1 with none, 1 with 2 constraints.
        // 2 max constraints are satisfied (default int value is 0)
        assertEquals(3, result.size());
        List<Annotation> annotations = result.stream().map(it ->
            it.getConstraintDescriptor().getAnnotation()
        ).collect(Collectors.toList());

        TODO all checks
    }
}