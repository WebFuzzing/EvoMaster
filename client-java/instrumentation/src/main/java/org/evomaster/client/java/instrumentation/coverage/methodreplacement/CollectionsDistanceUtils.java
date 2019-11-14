package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.heuristic.Truthness;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;

public abstract class CollectionsDistanceUtils {

    public static double getHeuristicToContains(Collection c, Object o) {
        Objects.requireNonNull(c);

        String inputString = null;
        if (o instanceof String) {
            inputString = (String) o;
        }

        Number inputNumber = null;
        if (o instanceof Number) {
            inputNumber = (Number) o;
        }

        Truthness t;

        boolean result = c.contains(o);
        if (result) {
            return 1d;
        } else {
            if (c.isEmpty()) {
                return DistanceHelper.H_REACHED_BUT_EMPTY;
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
                return max;
            }
        }
    }
}
