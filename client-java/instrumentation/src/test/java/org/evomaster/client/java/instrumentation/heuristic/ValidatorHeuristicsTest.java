package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.instrumentation.heuristic.validator.BaseConstraintsBean;
import org.evomaster.client.java.instrumentation.heuristic.validator.NoConstraintsBean;
import org.evomaster.client.java.instrumentation.heuristic.validator.SingleConstraintBean;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import javax.validation.metadata.BeanDescriptor;

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

        BeanDescriptor descriptor = validator.getConstraintsForClass(BaseConstraintsBean.class);
        assertTrue(descriptor.isBeanConstrained());
        assertEquals(4, descriptor.getConstrainedProperties().size());
        assertEquals(5, descriptor.getConstrainedProperties().stream()
                .flatMap(it -> it.getConstraintDescriptors().stream())
                .count()
        );

        Set<ConstraintViolation<BaseConstraintsBean>> result =  validator.validate(bean);
        //5 constraints, over 4 fields, 1 with none, 1 with 2 constraints.
        // 2 max constraints are satisfied (default int value is 0)
        assertEquals(3, result.size());
    }

    @Test
    public void testSingleConstraintBean(){

        SingleConstraintBean bean = new SingleConstraintBean();

        BeanDescriptor descriptor = validator.getConstraintsForClass(SingleConstraintBean.class);
        assertTrue(descriptor.isBeanConstrained());
        assertEquals(1, descriptor.getConstrainedProperties().size());
        assertEquals(1, descriptor.getConstrainedProperties().stream()
                .flatMap(it -> it.getConstraintDescriptors().stream())
                .count()
        );

        bean.x = 42;

        Set<ConstraintViolation<SingleConstraintBean>> result =  validator.validate(bean);
        assertEquals(0, result.size());

        bean.x = -5;

        result =  validator.validate(bean);
        assertEquals(1, result.size());

        ConstraintViolation<SingleConstraintBean> failure = result.stream().findFirst().get();
        assertEquals(-5, failure.getInvalidValue());
        assertEquals(Min.class, failure.getConstraintDescriptor().getAnnotation().annotationType());
        assertEquals(1L, failure.getConstraintDescriptor().getAttributes().get("value"));
    }


    @Test
    public void testHeuristicForSingleConstraintBean(){

        SingleConstraintBean bean = new SingleConstraintBean(); //Min 1
        bean.x = 42;

        Truthness t = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t.isTrue());
        assertFalse(t.isFalse());

        bean.x = -100;
        Truthness tm100 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertFalse(tm100.isTrue());
        assertTrue(tm100.isFalse());

        bean.x = -5;
        Truthness tm5 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertFalse(tm5.isTrue());
        assertTrue(tm5.isFalse());

        assertTrue(tm5.getOfTrue() > tm100.getOfTrue());


        bean.x = 1;
        Truthness t1 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t1.isTrue());
        assertFalse(t1.isFalse());
    }
}