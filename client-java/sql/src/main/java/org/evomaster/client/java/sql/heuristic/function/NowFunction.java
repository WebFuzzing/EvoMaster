package org.evomaster.client.java.sql.heuristic.function;

import java.sql.Timestamp;
import java.time.Instant;

public class NowFunction extends SqlFunction {

    public static final Timestamp CANONICAL_NOW_VALUE = java.sql.Timestamp.from(Instant.EPOCH);

    public NowFunction() {
        super("NOW");
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments.length != 0) {
            throw new IllegalArgumentException("NOW() function takes no argument");
        }
        Timestamp nowValue = CANONICAL_NOW_VALUE;
        return nowValue;
    }

}
