package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.TruthnessHelper;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;

public class CollectionClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Collection.class;
    }


    /**
     * @param c
     * @param o
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean contains(Collection c, Object o, String idTemplate) {
        return containsHelper(c, o, idTemplate);
    }

    protected static boolean containsHelper(Collection c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        String inputString = null;
        if (o instanceof String) {
            inputString = (String) o;
        }

        if (ExecutionTracer.isTaintInput(inputString)) {
            for (Object value : c) {
                if (value instanceof String) {
                    ExecutionTracer.addStringSpecialization(inputString,
                            new StringSpecializationInfo(StringSpecialization.CONSTANT, (String) value));
                }
            }
        }

        boolean result = c.contains(o);

        if (idTemplate == null) {
            return result;
        }

        Number inputNumber = null;
        if (o instanceof Number) {
            inputNumber = (Number) o;
        }

        Truthness t;

        if (result) {
            t = new Truthness(1d, 0d);
        } else {
            if (c.isEmpty()) {
                t = new Truthness(DistanceHelper.H_REACHED_BUT_EMPTY, 1d);
            } else {
                double max = DistanceHelper.H_REACHED_BUT_EMPTY;

                for (Object value : c) {
                    double distance = -1;

                    // String
                    if (inputString != null && value instanceof String) {
                        distance = (double) DistanceHelper.getLeftAlignmentDistance(inputString, (String) value);
                    }
                    // Number
                    if (inputNumber != null && value instanceof Number) {
                        // Byte
                        if (inputNumber instanceof Byte && value instanceof Byte) {
                            distance = DistanceHelper.getDistanceToEquality(inputNumber.longValue(), ((Byte) value).longValue());
                        }
                        // Short
                        if (inputNumber instanceof Short && value instanceof Short) {
                            distance = DistanceHelper.getDistanceToEquality(inputNumber.longValue(), ((Short) value).longValue());
                        }
                        // Integer
                        if (inputNumber instanceof Integer && value instanceof Integer) {
                            distance = DistanceHelper.getDistanceToEquality(inputNumber.longValue(), ((Integer) value).longValue());
                        }
                        // Long
                        if (inputNumber instanceof Long && value instanceof Long) {
                            distance = DistanceHelper.getDistanceToEquality(inputNumber.longValue(), ((Long) value).longValue());
                        }
                        // Float
                        if (inputNumber instanceof Float && value instanceof Float) {
                            distance = DistanceHelper.getDistanceToEquality(inputNumber.doubleValue(), ((Float) value).doubleValue());
                        }
                        // Double
                        if (inputNumber instanceof Double && value instanceof Double) {
                            distance = DistanceHelper.getDistanceToEquality(inputNumber.doubleValue(), ((Double) value).doubleValue());
                        }
                    }
                    // Character
                    if (o instanceof Character && value instanceof Character) {
                        distance = (double) DistanceHelper.getLeftAlignmentDistance(((Character) o).toString(), ((Character) value).toString());
                    }
                    // Date
                    if (o instanceof Date && value instanceof Date) {
                        distance = DistanceHelper.getDistanceToEquality((Date) o, (Date) value);
                    }

                    if (distance > 0) {
                        final double base = DistanceHelper.H_NOT_EMPTY;
                        final double h = base + (1d - base) / (1d + distance);
                        if (h > max) {
                            max = h;
                        }
                    }
                }

                assert max < 1d;
                t = new Truthness(max, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

        return result;
    }

    /**
     * This function is called only when the caller is non-null.
     * The heuristic value is 1/(1+c.size()) where c!=null.
     * <p>
     * The closer the heuristic value is to 1, the closer the collection
     * is of being empty.
     *
     * @param caller     a non-null Collection instance
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean isEmpty(Collection caller, String idTemplate) {
        Objects.requireNonNull(caller);

        boolean result = caller.isEmpty();
        if (idTemplate == null) {
            return result;
        }

        int len = caller.size();
        Truthness t = TruthnessHelper.getTruthnessToEmpty(len);

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }
}
