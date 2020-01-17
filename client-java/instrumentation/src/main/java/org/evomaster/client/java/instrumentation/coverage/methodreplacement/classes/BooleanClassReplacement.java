package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.H_REACHED_BUT_NULL;

public class BooleanClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Boolean.class;
    }

    /**
     * The heuristic value is H_REACHED_BUT_NULL if the input string is null.
     * Otherwise, the leftAlignment distance to "true" is computed and added as
     * H_NOT_NULL + (1-H_NOT_NULL)/(1+distance) where distance is greater or equal to 0.
     * <p>
     * The closer the heuristic value is to 1, the closer it is to returning true.
     *
     * @param input
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN, replacingStatic = true)
    public static boolean parseBoolean(String input, String idTemplate) {

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.BOOLEAN, null));
        }

        if (idTemplate == null) {
            return Boolean.parseBoolean(input);
        }

        boolean res = Boolean.parseBoolean(input);
        Truthness t;
        if (res) {
            t = new Truthness(1, 0);
        } else {
            if (input == null) {
                t = new Truthness(H_REACHED_BUT_NULL, 1);
            } else {
                final double base = DistanceHelper.H_NOT_NULL;
                long distance = DistanceHelper.getLeftAlignmentDistance(input.toLowerCase(), "true");
                double h = base + ((1d - base) / (1d + distance));
                t = new Truthness(h, 1);
            }
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return res;
    }

    @Replacement(type = ReplacementType.BOOLEAN, replacingStatic = true)
    public static boolean valueOf(String input, String idTemplate) {
        return parseBoolean(input,idTemplate);
    }

}