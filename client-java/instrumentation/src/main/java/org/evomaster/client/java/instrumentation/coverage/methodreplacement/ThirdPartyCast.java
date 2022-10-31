package org.evomaster.client.java.instrumentation.coverage.methodreplacement;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ThirdPartyCast {

    /**
     * Specify class this input Object is actually representing.
     * This is done to avoid having third-party objects in method signatures of method replacements
     */
    String actualType();
}
