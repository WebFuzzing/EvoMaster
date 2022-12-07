package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Heuristics calculation for Java Beans, when dealing with javax.validation constraints
 */
public class ValidatorHeuristics {

    /**
     *
     * @param validator  A Object reference to a javax.validation.Validator instance.
     *                   Note: it is passed as Object, as we cannot have any compilation dependency on
     *                         validation library (and so need to use reflection)
     * @param bean   An object instance for a bean to validate
     * @return
     */
    public static Truthness computeTruthness(Object validator, Object bean){

        Objects.requireNonNull(validator);
        Objects.requireNonNull(bean);

        Class<?> validatorClass = validator.getClass();
        Class<?> beanClass = bean.getClass();

        try {
            Object beanDescriptor = validatorClass.getMethod("getConstraintsForClass", Class.class)
                    .invoke(validator, beanClass);
            boolean isConstrained = (Boolean) beanDescriptor.getClass().getMethod("isBeanConstrained")
                    .invoke(beanDescriptor);
            if(!isConstrained){
                throw new IllegalArgumentException("Bean has no constraints: " + beanClass.getName());
            }

            int n = getNumberOfTotalConstraints(beanDescriptor);

            Set<Object> constraintViolations = (Set<Object>) validatorClass.getMethod("validate", Object.class, Class[].class)
                    .invoke(validator, bean, new Class[]{});

            assert constraintViolations.size() <= n; //this will fail if we do not handle all types of constraints

            double solved = n - constraintViolations.size();

            for(Object violation : constraintViolations){
                double h = computeHeuristicToSolveFailedConstraint(violation);
                assert h>=0 && h<=1;
                solved += h;
            }

            if(constraintViolations.isEmpty()){
                // TODO handle heuristics for gradient to fail constraints
                return new Truthness(1, DistanceHelper.H_NOT_EMPTY);
            } else {
                double t = solved / (double) n;
                return new Truthness(t, 1d);
            }


        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
            throw new RuntimeException(e);
        }
    }

    private static double computeHeuristicToSolveFailedConstraint(Object violation) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //input must be of type ConstraintViolation

        Object invalidValue = violation.getClass().getMethod("getInvalidValue").invoke(violation);
        Object descriptor = violation.getClass().getMethod("getConstraintDescriptor").invoke(violation);

        Map<String,Object> attributes = (Map<String,Object>) descriptor.getClass().getMethod("getAttributes").invoke(descriptor);
        Annotation annotation = (Annotation) descriptor.getClass().getMethod("getAnnotation").invoke(descriptor);
        String annotationType = annotation.annotationType().getName();

           /*
        TODO handle all annotations
        TODO future ll need to handle Jakarta as well

AssertFalse
AssertTrue
DecimalMax
DecimalMin
Digits
Email
Future
FutureOrPresent
NotBlank
NotEmpty
NotNull
Null
Past
PastOrPresent
Pattern
Size
             */

        if(annotationType.equals("javax.validation.constraints.Min")){
            return computeHeuristicForMin(invalidValue, attributes);
        }
        if(annotationType.equals("javax.validation.constraints.Max")){
            return computeHeuristicForMax(invalidValue, attributes);
        }
        if(annotationType.equals("javax.validation.constraints.Positive")){
            return computeHeuristicForPositive(invalidValue, attributes);
        }
        if(annotationType.equals("javax.validation.constraints.PositiveOrZero")){
            return computeHeuristicForPositiveOrZero(invalidValue, attributes);
        }
        if(annotationType.equals("javax.validation.constraints.Negative")){
            return computeHeuristicForNegative(invalidValue, attributes);
        }
        if(annotationType.equals("javax.validation.constraints.NegativeOrZero")){
            return computeHeuristicForNegativeOrZero(invalidValue, attributes);
        }


        SimpleLogger.warn("Not able to handle constrain type: " + annotationType);
        return 0.5; //actual value here does not matter, as long as positive and less than 1
    }

    private static double computeHeuristicForMin(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double min = ((Number)attributes.get("value")).doubleValue();

        assert  x < min; //otherwise it would had not been a violation

        double distance = min - x;

        return DistanceHelper.heuristicFromScaledDistanceWithBase(0, distance);
    }

    private static double computeHeuristicForMax(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double max = ((Number)attributes.get("value")).doubleValue();

        assert  x > max; //otherwise it would had not been a violation

        double distance = x - max;

        return DistanceHelper.heuristicFromScaledDistanceWithBase(0, distance);
    }

    private static double computeHeuristicForPositive(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double min = 0d;

        assert  x <= min; //otherwise it would had not been a violation

        double distance = 1d + (min - x);  // we add a base because 0 is not a valid value

        return DistanceHelper.heuristicFromScaledDistanceWithBase(0, distance);
    }

    private static double computeHeuristicForPositiveOrZero(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double min = 0d;

        assert  x < min; //otherwise it would had not been a violation

        double distance = (min - x);

        return DistanceHelper.heuristicFromScaledDistanceWithBase(0, distance);
    }

    private static double computeHeuristicForNegative(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double max = 0d;

        assert  x >= max; //otherwise it would had not been a violation

        double distance = 1d + (x-max);  // we add a base because 0 is not a valid value

        return DistanceHelper.heuristicFromScaledDistanceWithBase(0, distance);
    }

    private static double computeHeuristicForNegativeOrZero(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double max = 0d;

        assert  x > max; //otherwise it would had not been a violation

        double distance = x-max;

        return DistanceHelper.heuristicFromScaledDistanceWithBase(0, distance);
    }

    private static int getNumberOfTotalConstraints(Object beanDescriptor) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        //TODO handle constraints on whole class

        Set<Object> properties = (Set<Object>) beanDescriptor.getClass().getMethod("getConstrainedProperties")
                .invoke(beanDescriptor);
        long n = properties.stream()
                .flatMap(it -> {
                    Set<Object> constraints = null;
                    try {
                        constraints = (Set<Object>) it.getClass().getMethod("getConstraintDescriptors")
                                        .invoke(it);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return constraints.stream();
                })
                .count();

        return (int) n;
    }



}
