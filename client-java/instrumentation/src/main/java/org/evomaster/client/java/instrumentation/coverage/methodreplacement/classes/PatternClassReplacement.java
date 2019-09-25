package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.PatternMatchingHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.regex.Pattern;


public class PatternClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return Pattern.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN, replacingStatic = true)
    public static boolean matches(String regex, CharSequence input, String idTemplate) {
        if (regex == null || input == null) {
            // if any of the required inputs is null, throw a NPE
            return Pattern.matches(regex, input);
        } else {
            // otherwise use the helper clase for pattern matching distances
            return PatternMatchingHelper.matches(regex, input.toString(), idTemplate);
        }
    }
}