package org.evomaster.client.java.sql.heuristic.function;

/**
 * Represents the SQL COALESCE() function, which returns the first non-null argument.
 */
public class CoalesceFunction extends SqlFunction {

    public CoalesceFunction() {
        super("COALESCE");
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments.length ==0) {
            throw new IllegalArgumentException("COALESCE() function requires at least one argument");
        }

        final Object arg = returnFirstNonNullArgument(arguments);
        return arg;
    }

    /**
     * Returns the first non-null argument from <code>arguments</code>.
     * If none of them is non-null, returns null.
     *
     * @param arguments
     * @return
     */
    private static Object returnFirstNonNullArgument(Object[] arguments) {
        for (Object arg : arguments) {
            if (arg != null) {
                return arg;
            }
        }
        return null;
    }

}
