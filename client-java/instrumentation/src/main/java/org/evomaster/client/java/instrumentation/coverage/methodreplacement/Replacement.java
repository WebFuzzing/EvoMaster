package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a static method as a replacement one for a method in the Java API.
 * As boolean values and exceptions lead to fitness plateaus, we need testability
 * transformations to give gradient, eg by creating new coverage targets,
 * in which we want to return true and false.
 * <br>
 * A {@code Replacement} method MUST have the same return type, and
 * match name/signature of replaced method, but last parameter being a
 * {@code String} id template.
 * However, if replacing a non-static method, then the replacement MUST
 * still be static, and have as first parameter the caller.
 * <br>
 * If the id template is {@code null}, then no new target should be registered.
 * However, we might still want to do taint analysis.
 * This is the case when instrumenting third-party libraries.
 * <br>
 * In the case of TRACKER methods, no id template is used, as those will never
 * be testing targets. Furthermore, the first parameter must always be of
 * type Object if it is from a third-party library
 * <br>
 * For replacement in third-party libraries, use {@link ThirdPartyMethodReplacementClass}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Replacement {

    /**
     * Specify if the target replaced method was static
     */
    boolean replacingStatic() default false;

    /**
     * There might be different reasons to replace a methods,
     * like dealing with methods that return booleans, or might throw
     * an exception
     */
    ReplacementType type();

    /**
     * Give an id to this replacement method. This is used to then easily
     * access the original replaced method via reflection
     */
    String id() default ""; //very annoyingly, Java does not allow null here :(
}
