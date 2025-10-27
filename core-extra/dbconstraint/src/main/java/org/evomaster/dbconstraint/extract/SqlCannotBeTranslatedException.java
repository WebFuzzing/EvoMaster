package org.evomaster.dbconstraint.extract;

public class SqlCannotBeTranslatedException extends RuntimeException {

    public SqlCannotBeTranslatedException(String message) {
        super(message);
    }
}
