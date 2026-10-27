package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts a replacement to calls made from the listed classes.
 * Class names use the fully qualified Java form, with dots between packages.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReplacementCallSites {

    String[] value();
}
