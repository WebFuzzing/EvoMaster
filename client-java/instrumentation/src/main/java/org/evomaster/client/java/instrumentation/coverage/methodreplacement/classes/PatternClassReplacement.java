package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.regex.Pattern;


public class PatternClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return Pattern.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN, replacingStatic = true, category = ReplacementCategory.BASE)
    public static boolean matches(String regex, CharSequence input, String idTemplate) {

        if (regex == null || input == null) {
            if (idTemplate == null) {
                // if no idTemplate, simply continue the execution
                return Pattern.matches(regex, input);
            }
            // otherwise, the heuristic value should be reached, but null.
            Truthness t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

            // if any of the required inputs is null, throw a NPE
            return Pattern.matches(regex, input);
        } else {
            // otherwise use the helper class for pattern matching distances
            return PatternMatchingHelper.matches(regex, input.toString(), idTemplate);
        }
    }
}