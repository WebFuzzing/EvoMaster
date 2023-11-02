package org.evomaster.testoldlibraries.heuristic;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.ValidatorHeuristics;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.testoldlibraries.heuristic.validator.PatternBean;
import org.evomaster.testoldlibraries.heuristic.validator.RangeBean;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationHeuristicsJiruktaTest {


    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testRangeBeanProperties(){

        RangeBean bean = new RangeBean();

        BeanDescriptor descriptor = validator.getConstraintsForClass(RangeBean.class);
        assertTrue(descriptor.isBeanConstrained());
        assertEquals(1, descriptor.getConstrainedProperties().size());
        assertEquals(1, descriptor.getConstrainedProperties().stream()
                .flatMap(it -> it.getConstraintDescriptors().stream())
                .count()
        );

        Set<ConstraintViolation<RangeBean>> result = validator.validate(bean);
        assertEquals(0, result.size());

        bean.list.add(55);
        bean.list.add(55);
        bean.list.add(123);
        bean.list.add(1);
        bean.list.add(2);

        result = validator.validate(bean);
        assertEquals(1, result.size()); //reported as single violation
    }

    @Test
    public void testRangeBean(){

        RangeBean bean = new RangeBean();

        Truthness t0 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t0.isTrue());

        bean.list.add(55);
        bean.list.add(55);
        bean.list.add(123);
        bean.list.add(1);
        bean.list.add(2);

        Truthness t1 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertFalse(t1.isTrue());

        bean.list.add(10);
        Truthness t2= ValidatorHeuristics.computeTruthness(validator, bean);
        assertEquals(t1.getOfTrue(), t2.getOfTrue(), 0.0001); // adding valid values should have no impact

        bean.list.set(0, 20);
        Truthness t3= ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t3.getOfTrue() > t2.getOfTrue()); //improvement although still 3 failing

        bean.list.remove(2);
        Truthness t4= ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t4.getOfTrue() > t3.getOfTrue()); //as removing one failing

        bean.list.set(0,1);
        bean.list.set(1,10);
        Truthness t5= ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t5.isTrue());
    }


    @Test
    public void testPatternBeanProperties(){

        PatternBean bean = new PatternBean();


        BeanDescriptor descriptor = validator.getConstraintsForClass(PatternBean.class);
        assertTrue(descriptor.isBeanConstrained());
        assertEquals(1, descriptor.getConstrainedProperties().size());
        assertEquals(1, descriptor.getConstrainedProperties().stream()
                .flatMap(it -> it.getConstraintDescriptors().stream())
                .count()
        );

        Set<ConstraintViolation<PatternBean>> result = validator.validate(bean);
        assertEquals(0, result.size());

        bean.list.add("fd");
        bean.list.add("5");
        bean.list.add("22");
        bean.list.add("a");
        bean.list.add("aaaaa");

        result = validator.validate(bean);
        assertEquals(1, result.size()); //reported as single violation
    }

    @Test
    public void testPatternBean() {

        PatternBean bean = new PatternBean();

        Truthness t0 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t0.isTrue());

        bean.list.add("fd");
        bean.list.add("5");
        bean.list.add("22");
        bean.list.add("a");
        bean.list.add("aaaaa");

        Truthness t1 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertFalse(t1.isTrue());

        bean.list.set(0, "aaaaaaaaaaaaaaa");
        Truthness t2 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t2.getOfTrue() > t1.getOfTrue());

        bean.list.add(TaintInputName.getTaintName(0));
        Truthness t3 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t2.getOfTrue() > t3.getOfTrue()); //worse
        assertEquals(t1.getOfTrue(), t3.getOfTrue(), 0.00001);

        bean.list.set(1,"a");
        bean.list.set(2,"aa");
        bean.list.set(5,"a");
        Truthness t4 = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t4.isTrue());
    }
}
