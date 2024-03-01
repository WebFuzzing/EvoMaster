package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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


    private static final String PREFIX_JAVAX = "javax.validation.constraints.";
    private static final String PREFIX_JAKARTA = "TODO"; //TODO
    private static final String PREFIX_HIBERNATE = "org.hibernate.validator.constraints.";
    private static final String PREFIX_JIRUKTA = "cz.jirutka.validator.collection.constraints.";

    /**
     * The original pattern "^([A-Za-z_-\+\.]){2,}@[A-Za-z]+(\.([A-Za-z]{2,})+)+$"
     * was not correctly handled by RegexHandler.createGeneForJVM().
     * This version is identical in terms of the valid character sequences.
     */
    public static final String EMAIL_REGEX_PATTERN = "^([A-Za-z_-]|\\.|\\+){2,}@[A-Za-z]+(\\.([A-Za-z]{2,})+)+$";
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

            n = n * 1000; // heuristics to handle collection constrains
                          // note that actual value of n does not matter, as long as equation gives gradient

            Set<Object> constraintViolations = (Set<Object>) validatorClass.getMethod("validate", Object.class, Class[].class)
                    .invoke(validator, bean, new Class[]{});

            /*
                This was not true... a collection constraint count as one, but each single element
                in the collection can create a violation
             */
            //assert constraintViolations.size() <= n; //this will fail if we do not handle all types of constraints
            double solved = Math.max(0 , n - constraintViolations.size());

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

        boolean javax = annotationType.startsWith(PREFIX_JAVAX);
        boolean jakarta = annotationType.startsWith(PREFIX_JAKARTA);
        boolean hibernate = annotationType.startsWith(PREFIX_HIBERNATE);
        boolean jirukta = annotationType.startsWith(PREFIX_JIRUKTA);

        if(!javax && !jakarta && !hibernate && !jirukta) {
            SimpleLogger.warn("Not recognized constraint library. Not able to handle constraint type: " + annotationType);
            return defaultFailed;
        }


           /*
        TODO handle all annotations
        TODO future ll need to handle Jakarta namespace as well
        TODO hibernate and jirutka as well

MISSING javax.
DecimalMax
DecimalMin
Digits
Future
FutureOrPresent
Past
PastOrPresent

MISSING Hibernate
CNPJ
CPF
TituloEleitoral
NIP
PESEL
REGON
INN
DurationMax
DurationMin
CodePointLength
ConstraintComposition
CreditCardNumber
Currency
EAN
Email
ISBN
Length
LuhnCheck
Mod10Check
Mod11Check
ModCheck
Normalized
NotBlank
NotEmpty
ParameterScriptAssert
ScriptAssert
UniqueElements
URL

MISSING Jakarta
TODO
             */

        if(jirukta){
            return handleJiruktaConstraint(annotationType, invalidValue, attributes);
        }

        if(hibernate){
            if(annotationType.endsWith(".Range")){
                return computeHeuristicForRange(invalidValue, attributes);
            }
        }

        if(javax) {
            //Numeric constraints. Note that null values are valid here
            if (annotationType.endsWith(".Min")) {
                return computeHeuristicForMin(invalidValue, attributes);
            }
            if (annotationType.endsWith(".Max")) {
                return computeHeuristicForMax(invalidValue, attributes);
            }
            if (annotationType.endsWith(".Positive")) {
                return computeHeuristicForPositive(invalidValue, attributes);
            }
            if (annotationType.endsWith(".PositiveOrZero")) {
                return computeHeuristicForPositiveOrZero(invalidValue, attributes);
            }
            if (annotationType.endsWith(".Negative")) {
                return computeHeuristicForNegative(invalidValue, attributes);
            }
            if (annotationType.endsWith(".NegativeOrZero")) {
                return computeHeuristicForNegativeOrZero(invalidValue, attributes);
            }
            if (annotationType.endsWith(".Size")) {
                return computeHeuristicForSize(invalidValue, attributes);
            }


            //no gradient, apart from rewarding non-null
            if (annotationType.endsWith(".NotEmpty")
                    || annotationType.endsWith(".NotBlank")
            ) {
                return computeHeuristicForNoGradientButNullIsNotValid(invalidValue);
            }

            //no gradient and null can be valid
            if (annotationType.endsWith(".Null")
                    || annotationType.endsWith(".NotNull")
                    || annotationType.endsWith(".AssertTrue")
                    || annotationType.endsWith(".AssertFalse")
            ) {
                return defaultFailed;
            }

            if (annotationType.endsWith(".Pattern")
                || annotationType.endsWith(".Email")) {
            /*
                Quite expensive to handle, see RegexDistanceUtils.
                so, for now, we just ensure we handle taint analysis for this
             */
                assert invalidValue != null; // otherwise would had been valid
                String value = invalidValue.toString();
                if (ExecutionTracer.isTaintInput(value)) {
                    final String pattern;
                    if (annotationType.endsWith(".Pattern")) {
                        pattern = attributes.get("regexp").toString();
                    } else {
                        assert(annotationType.endsWith(".Email"));
                        pattern = EMAIL_REGEX_PATTERN;
                    }

                    ExecutionTracer.addStringSpecialization(value,
                            new StringSpecializationInfo(StringSpecialization.REGEX_WHOLE,
                                    pattern));
                }

                return defaultFailed;
            }

        }

        SimpleLogger.warn("Not able to handle constrain type: " + annotationType);
        return defaultFailed;
    }

    private static double handleJiruktaConstraint(String annotationType, Object invalidValue, Map<String, Object> attributes) {

       /*
        Old library, which is deprecated. Recent javax.validation can handle collections.
        TODO: So, handling all cases here is likely low priority.
        Originally handled due its use in OCVN.

        Note: tests for these are in different module, to handle conflicting libraries.
        See ValidationHeuristicsJiruktaTest

       MISSING  JIRUKTA
EachAssertFalse
EachAssertTrue
EachConstraint
EachCreditCardNumber
EachDecimalMax
EachDecimalMin
EachDigits
EachEAN
EachEmail
EachFuture
EachLength
EachLuhnCheck
EachMax
EachMin
EachMod10Check
EachMod11Check
EachNotBlank
EachNotEmpty
EachNotNull
EachPast
EachSafeHtml
EachScriptAssert
EachSize
EachURL
        */

        if(annotationType.endsWith(".EachRange")){
            Collection<Integer> values = (Collection<Integer>) invalidValue;

            long sum = 0;

            for(Integer k : values){
                sum += getDistanceForRange(k, attributes);
            }

            return DistanceHelper.heuristicFromScaledDistanceWithBase(DistanceHelper.H_NOT_NULL, sum);
        }

        if(annotationType.endsWith(".EachPattern")) {

            Collection<String> values = (Collection<String>) invalidValue;
            String regexp = attributes.get("regexp").toString();

            int mismatches = 0;

            for(String value : values) {
                if (ExecutionTracer.isTaintInput(value)) {
                    ExecutionTracer.addStringSpecialization(value,
                            new StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, regexp));
                }

                boolean matched = value.matches(regexp);
                if(!matched){
                    mismatches++;
                }
            }

            assert mismatches > 0;

            return DistanceHelper.heuristicFromScaledDistanceWithBase(DistanceHelper.H_NOT_NULL, mismatches);
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

    private static double computeHeuristicForSize(Object invalidValue, Map<String, Object> attributes){

        assert invalidValue != null; //@Size is true on null element, so would not be a violation
        //however it is NOT the case for @NotEmpty where null fails.
        // ie @Size(min=0) != @NotEmpty

        Integer size = computeSize(invalidValue);

        if(size == null) {
            SimpleLogger.warn("Cannot handle @Size for type: " + invalidValue.getClass().getName());
            return DistanceHelper.H_NOT_NULL;
        }

        return computeHeuristicForRange(size, attributes);
    }

    private static int getDistanceForRange(Object invalidValue, Map<String, Object> attributes){

        int value = ((Number)invalidValue).intValue();

        int min = ((Number) attributes.get("min")).intValue();
        int max = ((Number) attributes.get("max")).intValue();

        if(min > max){
            //is this even possible?
            SimpleLogger.warn("Impossible to satisfy constraint min>max : " + min +">" + max);
            return -1;
        }

        return DistanceHelper.distanceToRange(value, min, max);
    }

    private static double computeHeuristicForRange(Object invalidValue, Map<String, Object> attributes) {

        int distance = getDistanceForRange(invalidValue, attributes);
        if(distance < 0){ //this could happen if min/max are invalid
            return DistanceHelper.H_NOT_NULL;
        }
        assert distance != 0;

        return DistanceHelper.heuristicFromScaledDistanceWithBase(DistanceHelper.H_NOT_NULL, distance);
    }


    private static Integer computeSize(Object invalidValue){

        int size;

        if(invalidValue instanceof CharSequence){
            size = ((CharSequence) invalidValue).length();
        } else if(invalidValue instanceof Collection){
            size = ((Collection<?>) invalidValue).size();
        } else if(invalidValue instanceof Map){
            size = ((Map<?,?>)invalidValue).size();
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
                .mapToInt(it -> {
                    Set<Object> constraints = null;
                    Set<Object> collectionConstraints = null;
                    try {
                        constraints = (Set<Object>) it.getClass().getMethod("getConstraintDescriptors")
                                        .invoke(it);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        collectionConstraints = (Set<Object>) it.getClass().getMethod("getConstrainedContainerElementTypes")
                                .invoke(it);
                    } catch (NoSuchMethodException e){
                        //old versions of Hibernate do not have this method
                        collectionConstraints = new HashSet<>();
                    } catch (Exception e){
                        throw new RuntimeException(e);
                    }

                    return constraints.size() + collectionConstraints.size();
                })
                .sum();

        //for constraints on whole bean
        Set<Object> classConstraints = (Set<Object>) beanDescriptor.getClass().getMethod("getConstraintDescriptors")
                .invoke(beanDescriptor);
        n += classConstraints.size();

        return (int) n;
    }



}
