package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;

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
     *  Specify if the target to replace is a constructor call, ie, using "new".
     *  Note that constructors are handled very specially.
     *  Replacement must return void, and rather save the newly create instance locally.
     *  To avoid concurrency issue, should save it in a ThreadLocal.
     *  Such instance must then be returned in a static method with name same
     *  as what currently stored in MethodReplacementClass.CONSUME_INSTANCE_METHOD_NAME
     *  See further documentation there.
     *  Also, recall that most of these constraints are checked in ReplacementListTest
     *
     *  For an explanation of why we do this, look at the comments in MethodReplacementMethodVisitor
     */
    boolean replacingConstructor() default false;

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

    /**
     * Specify where the transformation can be applied.
     * Sometimes we might want transformations only in the SUT, or sometime only
     * in the third-party libraries.
     */
    UsageFilter usageFilter() default UsageFilter.ANY;

    /**
     * Whether the method has side-effects. This is important to check if we can
     * call it more than once without worries of changing a state
     */
    boolean isPure() default true;

    ReplacementCategory category();

    /**
     * Name of the class for which the return value of this method should be cast to.
     * This is necessary for 3rd-party replacements only.
     */
    String castTo() default "";

    /**
     * Method replacement will not be applied to classes in given prefix package.
     * If it starts with a '.', then it is not treated as prefix (ie match anywhere in the package full name).
     * This latter is useful when dealing with packages that are shaded
     */
    String[] packagesToSkip() default {};

    /**
     * Only applicable if UsageFilter is ONLY_SUT
     */
    String[] extraPackagesToConsider() default {};
}
