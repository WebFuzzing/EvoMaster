package org.evomaster.core.extra.shared;

/**
 * Information about a "testing-target" achieved (or not) during the execution of a test case.
 * Targets could be executed statements/branches in the source code, achieved HTTP status responses, etc.
 */
public class TargetInfo {
    /**
     * Unique identifier for this target.
     * The actual value does not matter, as long as it is unique and not changing throughout the search (ie, no time-stamps).
     * Descriptive ids are useful also during debugging.
     */
    private final String descriptiveId;

    /**
     * Heuristic value in range [0,1].
     * The value 1 means the target has been covered.
     * Otherwise, not covered, where values closer to 1 represent heuristically how close it was to cover them.
     */
    private final double value;

    /**
     * A test case could be composed of several actions (eg HTTP calls).
     * Here, _optionally_ we can keep track of in which action the target was covered.
     * If this information is missing or not collected, or the heuristic [value] is 0,
     * then this index must keep a negative value.
     */
    private final int actionIndex;

    public TargetInfo(String descriptiveId, double value) {
        this(descriptiveId, value, -1);
    }

    public TargetInfo(String descriptiveId, double value, int actionIndex) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Heuristic value must be in range [0..1]");
        }
        this.descriptiveId = descriptiveId;
        this.value = value;
        this.actionIndex = actionIndex;
    }

    public String getDescriptiveId() {
        return descriptiveId;
    }

    public double getValue() {
        return value;
    }

    public int getActionIndex() {
        return actionIndex;
    }
}