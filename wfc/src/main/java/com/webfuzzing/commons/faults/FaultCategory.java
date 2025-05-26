package com.webfuzzing.commons.faults;

import java.util.Objects;

public interface FaultCategory {


    /**
     * A unique code identifying this fault category
     */
    public int getCode();

    /**
     * A short descriptive name to explain the category
     */
    public String getDescriptiveName();

    /**
     * A short label to be used in test case naming when a single fault is found
     */
    public String getTestCaseLabel();

    /**
     * A descriptive identifier for this category.
     * Not a full, lengthy description.
     * For example based on code and name
     */
    public default String getLabel() {
        return "F" + getCode() + ":" + getDescriptiveName();
    }

}
