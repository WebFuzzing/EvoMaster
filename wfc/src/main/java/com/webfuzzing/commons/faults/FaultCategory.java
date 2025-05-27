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
     * A short label that can be used in test case naming
     */
    public String getTestCaseLabel();

    /**
     * A full, lengthy description of this fault category.
     * It should not contain any special formatting, as this field will be used for documentation
     * in different context, eg, markdown and HTML.
     */
    public String getFullDescription();

    /**
     * A descriptive identifier for this category.
     * Not a full, lengthy description.
     * For example based on code and name
     */
    public default String getLabel() {
        return "F" + getCode() + ":" + getDescriptiveName();
    }

}
