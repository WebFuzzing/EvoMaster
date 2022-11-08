package org.evomaster.client.java.instrumentation.heuristic;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

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




        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
            throw new RuntimeException(e);
        }

        return null; //TODO
    }

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
Max
Min
Negative
NegativeOrZero
NotBlank
NotEmpty
NotNull
Null
Past
PastOrPresent
Pattern
Positive
PositiveOrZero
Size
             */

}
