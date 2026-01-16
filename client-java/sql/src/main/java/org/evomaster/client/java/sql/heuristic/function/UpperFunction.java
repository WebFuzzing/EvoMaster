package org.evomaster.client.java.sql.heuristic.function;

public class UpperFunction extends SqlFunction {

    public UpperFunction() {
        super("UPPER");
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments.length !=1) {
            throw new IllegalArgumentException("UPPER() function takes exactly one argument but got: " + arguments.length);
        }

        Object concreteValue = arguments[0];
        if (concreteValue==null) {
            return null;
        } else if (concreteValue instanceof String) {
            return ((String) concreteValue).toUpperCase();
        } else {
            throw new IllegalArgumentException("UPPER() function takes a string argument, but got a " + concreteValue.getClass().getName());
        }
    }

}
