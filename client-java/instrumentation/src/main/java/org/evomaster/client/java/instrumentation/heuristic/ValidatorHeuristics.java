package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Heuristics calculation for Java Beans, when dealing with javax.validation constraints
 */
public class ValidatorHeuristics {


    /*
        actual value here does not matter, as long as positive and less than 1.
        however, need to make sure that, for constraints that are valid on null, they
        always get higher than this value (this can be achieved by using it as a base).
        Otherwise, can have nasty side-effects (see ValidatorHeuristicsTest for an example)
     */
    private static final double defaultFailed = 0.001;

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
        TODO future ll need to handle Jakarta namespace as well

DecimalMax
DecimalMin
Digits
Email
Future
FutureOrPresent
Past
PastOrPresent
Pattern
             */

        //Numeric constraints. Note that null values are valid here
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
        if(annotationType.equals("javax.validation.constraints.Size")){
            return computeHeuristicForSize(invalidValue, attributes);
        }


        //no gradient, apart from rewarding non-null
        if(annotationType.equals("javax.validation.constraints.NotEmpty")
                || annotationType.equals("javax.validation.constraints.NotBlank")
        ){
            return computeHeuristicForNoGradientButNullIsNotValid(invalidValue);
        }

        //no gradient and null can be valid
        if(annotationType.equals("javax.validation.constraints.Null")
            || annotationType.equals("javax.validation.constraints.NotNull")
                || annotationType.equals("javax.validation.constraints.AssertTrue")
                || annotationType.equals("javax.validation.constraints.AssertFalse")
        ){
            return defaultFailed;
        }

        if(annotationType.equals("javax.validation.constraints.Pattern")){
            /*
                Quite expensive to handle, see RegexDistanceUtils.
                so, for now, we just ensure we handle taint analysis for this
             */
            assert invalidValue != null; // otherwise would had been valid
            String value = invalidValue.toString();
            if(ExecutionTracer.isTaintInput(value)){
                ExecutionTracer.addStringSpecialization(value,
                        new StringSpecializationInfo(StringSpecialization.REGEX_WHOLE,
                                attributes.get("value").toString()));
            }

            return defaultFailed;
        }


        SimpleLogger.warn("Not able to handle constrain type: " + annotationType);
        return defaultFailed;
    }

    private static double computeHeuristicForNoGradientButNullIsNotValid(Object invalidValue){
        if(invalidValue == null){
            return DistanceHelper.H_REACHED_BUT_NULL;
        }

        return DistanceHelper.H_NOT_NULL;
    }


    private static double computeHeuristicForSize(Object invalidValue, Map<String, Object> attributes) {

        assert invalidValue != null; //@Size is true on null element, so would not be a violation
        //however it is NOT the case for @NotEmpty where null fails.
        // ie @Size(min=0) != @NotEmpty

        Integer size = computeSize(invalidValue);

        if(size == null) {
            SimpleLogger.warn("Cannot handle @Size for type: " + invalidValue.getClass().getName());
            return DistanceHelper.H_NOT_NULL;
        }

        int min = ((Number) attributes.get("min")).intValue();
        int max = ((Number) attributes.get("max")).intValue();

        if(min > max){
            //is this even possible?
            SimpleLogger.warn("Impossible to satisfy constraint min>max : " + min +">" + max);
            return DistanceHelper.H_NOT_NULL;
        }

        int distance = DistanceHelper.distanceToRange(size, min, max);
        assert distance > 0;

        return DistanceHelper.heuristicFromScaledDistanceWithBase(DistanceHelper.H_NOT_NULL, distance);
    }


    private static Integer computeSize(Object invalidValue){

        int size;

        if(invalidValue instanceof CharSequence){
            size = ((CharSequence) invalidValue).length();
        } else if(invalidValue instanceof Collection){
            size = ((Collection) invalidValue).size();
        } else if(invalidValue instanceof Map){
            size = ((Map)invalidValue).size();
        } else if(invalidValue.getClass().isArray()){
            size =  Array.getLength(invalidValue);
        } else {
            SimpleLogger.warn("Cannot compute size for type: " + invalidValue.getClass().getName());
            return null;
        }

        return size;
    }

    private static double computeHeuristicForMin(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double min = ((Number)attributes.get("value")).doubleValue();

        assert  x < min; //otherwise it would had not been a violation

        double distance = min - x;

        return DistanceHelper.heuristicFromScaledDistanceWithBase(defaultFailed, distance);
    }

    private static double computeHeuristicForMax(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double max = ((Number)attributes.get("value")).doubleValue();

        assert  x > max; //otherwise it would had not been a violation

        double distance = x - max;

        return DistanceHelper.heuristicFromScaledDistanceWithBase(defaultFailed, distance);
    }

    private static double computeHeuristicForPositive(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double min = 0d;

        assert  x <= min; //otherwise it would had not been a violation

        double distance = 1d + (min - x);  // we add a base because 0 is not a valid value

        return DistanceHelper.heuristicFromScaledDistanceWithBase(defaultFailed, distance);
    }

    private static double computeHeuristicForPositiveOrZero(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double min = 0d;

        assert  x < min; //otherwise it would had not been a violation

        double distance = (min - x);

        return DistanceHelper.heuristicFromScaledDistanceWithBase(defaultFailed, distance);
    }

    private static double computeHeuristicForNegative(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double max = 0d;

        assert  x >= max; //otherwise it would had not been a violation

        double distance = 1d + (x-max);  // we add a base because 0 is not a valid value

        return DistanceHelper.heuristicFromScaledDistanceWithBase(defaultFailed, distance);
    }

    private static double computeHeuristicForNegativeOrZero(Object invalidValue, Map<String, Object> attributes) {

        double x = ((Number) invalidValue).doubleValue();
        double max = 0d;

        assert  x > max; //otherwise it would had not been a violation

        double distance = x-max;

        return DistanceHelper.heuristicFromScaledDistanceWithBase(defaultFailed, distance);
    }

    private static int getNumberOfTotalConstraints(Object beanDescriptor) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

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

        //for constraints on whole bean
        Set<Object> classConstraints = (Set<Object>) beanDescriptor.getClass().getMethod("getConstraintDescriptors")
                .invoke(beanDescriptor);
        n += classConstraints.size();

        return (int) n;
    }



}
