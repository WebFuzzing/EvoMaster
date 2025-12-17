package org.evomaster.client.java.sql.heuristic.function;

public class CoalesceFunction extends SqlFunction {

    public CoalesceFunction() {
        super("COALESCE");
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments.length ==0) {
            throw new IllegalArgumentException("COALESCE() function requires at least one argument");
        }

        for (Object arg : arguments) {
            if (arg != null) {
                return arg;
            }
        }
        return null;
    }

}
