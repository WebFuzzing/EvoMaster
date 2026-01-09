package org.evomaster.client.java.sql.heuristic.function;

public class LowerFunction extends SqlFunction {

    public LowerFunction() {
        super("LOWER");
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments.length !=1) {
            throw new IllegalArgumentException("LOWER() function takes exactly one argument but got:" + arguments.length);
        }

        Object concreteValue = arguments[0];
        if (concreteValue==null) {
            return null;
        } else if (concreteValue instanceof String) {
            return ((String) concreteValue).toLowerCase();
        } else {
            throw new IllegalArgumentException("LOWER() function takes a string argument, but got a " + concreteValue.getClass().getName());
        }
    }

}
