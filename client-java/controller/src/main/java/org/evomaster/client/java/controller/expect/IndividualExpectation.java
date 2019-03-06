package org.evomaster.client.java.controller.expect;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

public interface IndividualExpectation {

    /**
     * An individual expectation from a call result.
     *
     * @param active is the expectation active. If active, and the condition is false, an Exception will be raised.
     * @param condition is the condition to be checked. If the expectation is not [active], the truth value
     *                  of the condition is not relevant.
     *
     * Additional functions can/will be developed.
    */
    IndividualExpectation that(boolean active, boolean condition);

    /**
     * An individual expectation from a call result.
     *
     * @param active is the expectation active. If active, and the condition is false, an Exception will be raised.
     * @param method is should be invoked if active (and any other conditions are met).
     *               The idea is to allow the use of Hamcrest Matchers and Rest Assured assertions.
     *
     * Additional functions can/will be developed.
     */

    IndividualExpectation that(boolean active, Method method, Object[] args);
}
