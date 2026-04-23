/**
 * Base interface used to control the system under test (SUT)
 * from the generated tests.
 * Needed base functionalities are, for example, starting/stopping
 * the SUT, and reset its state.
 */
export default interface SutHandler {

    /**
     * There might be different settings based on when the SUT is run during the
     * search of EvoMaster, and when it is later started in the generated tests.
     */
    setupForGeneratedTest(): Promise<void>;

    /**
     * Start a new instance of the SUT.
     * <br>
     * This method must be blocking until the SUT is initialized.
     *
     * @return the base URL of the running SUT, eg "http://localhost:8080"
     */
    startSut(): Promise<string>;

    /**
     * Stop the SUT
     */
    stopSut(): Promise<void>;

    /**
     * Make sure the SUT is in a clean state (eg, reset data in database).
     * A possible (likely very inefficient) way to implement this would be to
     * call #stopSUT followed by #startSUT
     */
    resetStateOfSUT(): Promise<void>;

    /**
     * Execute the given data insertions into the database (if any)
     *
     * @param insertions DTOs for each insertion to execute
     */
    // execInsertionsIntoDatabase(insertions: <InsertionDto>): void;
}
