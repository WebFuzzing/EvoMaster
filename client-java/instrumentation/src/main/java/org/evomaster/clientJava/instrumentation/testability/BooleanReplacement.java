package org.evomaster.clientJava.instrumentation.testability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a static method as a replacement one for a boolean method in the
 * Java API.
 * As boolean values lead to fitness plateaus, we need testability
 * transformations to give gradient, eg by returning
 * an integer representing the truthness of the method call,
 * ie like a branch distance.
 * <br>
 * A {@code BooleanReplacement} method MUST return an {@code int}, and
 * match name/signature of replaced method.
 * However, if replacing a non-static method, then the replacement MUST
 * still be static, and have as first parameter the caller
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BooleanReplacement {

    /**
     * Max positive value representing TRUE
     */
    int TRUE_MAX = Integer.MAX_VALUE - 2;

    /**
     * Min negative value representing FALSE
     */
    int FALSE_MIN = -TRUE_MAX;

    boolean replacingStatic() default false;
}
