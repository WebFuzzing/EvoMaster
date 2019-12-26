package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Objects;
import java.util.regex.Pattern;

public class PatternMatchingHelper {

    /**
     * Invocation to Pattern.matches() is free of side-effects.
     *
     * @param regex
     * @param input
     * @param idTemplate
     * @return
     */
    public static boolean matches(String regex, String input, String idTemplate) {
        Objects.requireNonNull(regex);
        Objects.requireNonNull(input);

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.REGEX, regex));
        }

        if (idTemplate == null) {
            return Pattern.matches(regex, input);
        }

        boolean matches = Pattern.matches(regex, input);
        if (matches) {
            ExecutionTracer.executedReplacedMethod(idTemplate,
                    ReplacementType.BOOLEAN,
                    new Truthness(1d, 0d));

        } else {
            int distance = RegexDistanceUtils.getStandardDistance(input.toString(), regex);
            ExecutionTracer.executedReplacedMethod(idTemplate,
                    ReplacementType.BOOLEAN,
                    new Truthness(1d / (1d + distance), 1d));
        }
        return matches;
    }
}
