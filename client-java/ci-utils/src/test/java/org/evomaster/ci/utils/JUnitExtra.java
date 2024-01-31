package org.evomaster.ci.utils;

import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.opentest4j.AssertionFailedError;


public class JUnitExtra {

    /**
     * Unfortunately, JUnit is not going to support it, which really sucks :(
     * so have to implement manually here
     *
     * https://github.com/junit-team/junit5/issues/2129
     *
     * @param expectedType
     * @param executable
     * @return
     * @param <T>
     */
    public static <T extends Throwable> T assertThrowsInnermost(Class<T> expectedType, Executable executable) {

        try {
            executable.execute();
        }
        catch (Throwable actualException) {
            Throwable cause = actualException;
            while(cause.getCause() != null){
                cause = cause.getCause();
            }

            if (expectedType.isInstance(cause)) {
                return (T) cause;
            }
            else {
                UnrecoverableExceptions.rethrowIfUnrecoverable(actualException);
                String message = "Unexpected cause exception type thrown: " + cause.getClass().getName();
                throw new AssertionFailedError(message, cause);
            }
        }

        String message = "Expected " + expectedType.getName() +" to be thrown as cause, but nothing was thrown.";
        throw new AssertionFailedError(message);
    }
}
