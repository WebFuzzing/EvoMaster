package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

/**
 * Mark a class to contain method replacements for APIs in the JDK
 */
public interface MethodReplacementClass {


    /**
     * When replacing constructors, instance is not returned (due to needed POP on stack),
     * and rather saved internally.
     * We then need a method to retrieve such instance.
     *
     * Note: this is not made as a method in this interface, because possible issues with
     * return types, and the fact it is not needed if constructor does not need to be replaced.
     * Furthermore, it is static in the subclasses.
     */
    public static final String CONSUME_INSTANCE_METHOD_NAME = "consumeInstance";

    /**
     * The target class in the JDK this class provides replacements for
     */
    Class<?> getTargetClass();


    default String getTargetClassName(){
        return getTargetClass().getName();
    }

    /**
     * Depending on the SUT, third-party libraries might not be present
     * on the classpath
     */
    default boolean isAvailable(){
        return getTargetClass() != null;
    }
}
