package org.evomaster.clientJava.controller;

/**
 * Base interface used to control the system under test (SUT)
 * from the generated tests.
 * Needed base functionalities are for example, starting/stopping
 * the SUT, and reset its state.
 */
public interface SutHandler {

    /**
     * Start a new instance of the SUT.
     * <br>
     * This method must be blocking.
     *
     * @return the base URL of the running SUT, eg "http://localhost:8080"
     */
    String startSut();

    /**
     * Stop the system under test
     */
    void stopSut();

    /**
     * Make sure the SUT is in a clean state (eg, reset data in database).
     * A possible (likely very inefficient) way to implement this would be to
     * call #stopSUT followed by #startSUT
     */
    void resetStateOfSUT();
}
