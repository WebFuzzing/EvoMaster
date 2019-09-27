package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.heuristic.Truthness;

public class TruthnessHelper {

    /**
     * @param len a positive value for a length
     * @return
     */
    public static Truthness getTruthnessToEmpty(int len) {
        Truthness t;
        if (len < 0) {
            throw new IllegalArgumentException("lengths should always be non-negative. Invalid length " + len);
        }
        if (len == 0) {
            t = new Truthness(1, 0);
        } else {
            t = new Truthness(1d / (1d + len), 1);
        }
        return t;
    }
}
