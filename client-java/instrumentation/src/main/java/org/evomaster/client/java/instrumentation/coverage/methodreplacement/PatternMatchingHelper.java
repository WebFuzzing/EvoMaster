package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternMatchingHelper {


    private static final int DEFAULT_PATTERN_FLAGS = 0;

    /**
     * Invocation to Pattern.matches() is free of side-effects.
     */
    public static boolean matches(String regex, String input, String idTemplate) {
        return matches(regex, DEFAULT_PATTERN_FLAGS, input, idTemplate);
    }
        /**
         * Invocation to Pattern.matches() is free of side-effects.
         */
    public static boolean matches(String regex, int flags, String input, String idTemplate) {
        Objects.requireNonNull(regex);
        Objects.requireNonNull(input);

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, regex));
        }

        Pattern p = Pattern.compile(regex, flags);
        Matcher m = p.matcher(input);
        boolean matches = m.matches();

        if (idTemplate == null) {
            return matches;
        }

        if (matches) {
            ExecutionTracer.executedReplacedMethod(idTemplate,
                    ReplacementType.BOOLEAN,
                    new Truthness(1d, DistanceHelper.H_NOT_NULL));

        } else {
            int distance = RegexDistanceUtils.getStandardDistance(input, regex);
            ExecutionTracer.executedReplacedMethod(idTemplate,
                    ReplacementType.BOOLEAN,
                    new Truthness(1d / (1d + distance), 1d));
        }
        return matches;
    }
}
