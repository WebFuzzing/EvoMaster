package org.evomaster.client.java.instrumentation.external;

import java.io.Serializable;

/**
 * The type of commands the SutController can send to the Java Agent
 * running on the external process of the SUT
 */
public enum Command implements Serializable {

    NEW_SEARCH, NEW_TEST, TARGETS_INFO, ACK, ACTION_INDEX, ADDITIONAL_INFO, UNITS_INFO
}
