package org.evomaster.client.java.instrumentation.coverage.methodreplacement;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ThirdPartyCast {

    public static final String SEPARATOR = "_EM_";

    public static final Pattern NAME_REGEX = Pattern.compile(".+"+SEPARATOR+"\\d+");

    /**
     * Specify class this input Object is actually representing.
     * This is done to avoid having third-party objects in method signatures of method replacements.
     *
     * However, when doing this, it might well happen that we end up with more than 1 method replacement
     * with same signature (with only distinction being this annotation applied to the input parameters).
     * This is now allowed on the JVM.
     * We need to use a different name. But such name must follow this regex:
     *
     * name_EM_\d+
     *
     * ie, the original name with _EM_x as suffix.
     */
    String actualType();
}
