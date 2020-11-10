package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.heuristic.Truthness;

import java.util.Collection;
import java.util.Objects;

public abstract class CollectionsDistanceUtils {

    public static double getHeuristicToContains(Collection c, Object o) {
        return getHeuristicToContains(c, o, -1);
    }

    /**
     * Compute distance of object from each one of the elements in the collection.
     * But look only up to limit elements.
     * A negative values means look at all elements
     */
    public static double getHeuristicToContains(Collection c, Object o, int limit) {
        Objects.requireNonNull(c);

        boolean result = c.contains(o);
        if (result) {
            return 1d;
        } else {
            if (c.isEmpty()) {
                return DistanceHelper.H_REACHED_BUT_EMPTY;
            } else {
                double max = DistanceHelper.H_REACHED_BUT_EMPTY;

                int counter = 0;

                for (Object value : c) {
                    if(counter == limit){
                        break;
                    }
                    counter++;

                    final double distance = DistanceHelper.getDistance(o, value);
                    final double base = DistanceHelper.H_NOT_EMPTY;
                    final double h = base + (1d - base) / (1d + distance);
                    if (h > max) {
                        max = h;
                    }
                }
                assert max < 1d;
                return max;
            }
        }
    }
}
