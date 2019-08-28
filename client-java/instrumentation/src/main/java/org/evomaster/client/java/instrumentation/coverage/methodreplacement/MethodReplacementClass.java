package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

/**
 * Mark a class to contain method replacements for APIs in the JDK
 */
public interface MethodReplacementClass {

    /**
     * The target class in the JDK this class provides replacements for
     */
    Class<?> getTargetClass();
}
