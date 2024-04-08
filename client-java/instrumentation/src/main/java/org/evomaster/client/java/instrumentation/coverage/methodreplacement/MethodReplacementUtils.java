package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.staticstate.MethodReplacementPreserveSemantics;

public class MethodReplacementUtils {

    public static boolean needToPreserverSemantics() {
        return MethodReplacementPreserveSemantics.shouldPreserveSemantics;
    }
}
