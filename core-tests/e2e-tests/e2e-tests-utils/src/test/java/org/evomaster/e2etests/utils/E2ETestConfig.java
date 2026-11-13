package org.evomaster.e2etests.utils;

import org.evomaster.core.output.OutputFormat;

import java.util.List;
import java.util.Objects;

/**
 * Inputs of an E2E run in {@link EnterpriseTestBase}, ie, how EvoMaster is run and
 * how its generated tests are compiled and executed.
 * Only the output folder, the test class name and the iterations are mandatory.
 */
public class E2ETestConfig {

    private final String outputFolderName;
    private final String fullClassName;
    private final int iterations;

    private List<String> terminations = null;
    private boolean createTests = true;
    private boolean handleFlakyWithTests = false;
    private int timeoutMinutes = 3;
    private OutputFormat outputFormat = OutputFormat.KOTLIN_JUNIT_5;

    public E2ETestConfig(String outputFolderName, String fullClassName, int iterations) {
        this.outputFolderName = Objects.requireNonNull(outputFolderName);
        this.fullClassName = Objects.requireNonNull(fullClassName);
        this.iterations = iterations;
    }

    /**
     * The label is used as output folder, and as test class name in the "org.bar" package
     */
    public E2ETestConfig(String label, int iterations) {
        this(label, "org.bar." + label, iterations);
    }

    /**
     * Suffixes of the test classes when the generated suite is split by faults
     */
    public E2ETestConfig withTerminations(List<String> terminations) {
        this.terminations = terminations;
        return this;
    }

    public E2ETestConfig withCreateTests(boolean createTests) {
        this.createTests = createTests;
        return this;
    }

    /**
     * The generated tests might contain flaky assertions, so they are not compiled nor run.
     * Unlike in the BB tests, they cannot be run in each flaky attempt, as a test class
     * loaded in this JVM cannot be compiled and loaded again.
     */
    public E2ETestConfig withHandleFlakyWithTests(boolean handleFlakyWithTests) {
        this.handleFlakyWithTests = handleFlakyWithTests;
        return this;
    }

    public E2ETestConfig withTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
        return this;
    }

    /**
     * Format of the generated tests, which are then compiled with the matching compiler
     */
    public E2ETestConfig withOutputFormat(OutputFormat outputFormat) {
        if (!outputFormat.isJavaOrKotlin() || !outputFormat.isJUnit5()) {
            //JUnit 4 would need the vintage engine, which is not on the e2e classpath, so the tests would compile but none would be run
            throw new IllegalArgumentException("Only JUnit 5 tests in Java or Kotlin can be compiled and run: " + outputFormat);
        }
        this.outputFormat = outputFormat;
        return this;
    }

    public String getOutputFolderName() {
        return outputFolderName;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public int getIterations() {
        return iterations;
    }

    public List<String> getTerminations() {
        return terminations;
    }

    public boolean isCreateTests() {
        return createTests;
    }

    public boolean isHandleFlakyWithTests() {
        return handleFlakyWithTests;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }
}
