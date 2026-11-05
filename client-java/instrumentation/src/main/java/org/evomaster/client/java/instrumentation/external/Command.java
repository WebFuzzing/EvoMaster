package org.evomaster.client.java.instrumentation.external;

import java.io.Serializable;

/**
 * The type of commands the SutController can send to the Java Agent
 * running on the external process of the SUT
 */
public enum Command implements Serializable {

    NEW_SEARCH,
    NEW_TEST,
    ALL_COVERED_TARGETS_INFO,
    TARGETS_INFO,
    ACK,
    ACTION_INDEX,
    ADDITIONAL_INFO,
    UNITS_INFO,
    KILL_SWITCH,
    EXECUTING_INIT_SQL,
    EXECUTING_INIT_MONGO,
    EXECUTING_INIT_CASSANDRA,
    EXECUTING_INIT_REDIS,
    EXECUTING_INIT_NEO4J,
    EXECUTING_INIT_DYNAMODB,
    EXECUTING_ACTION,
    BOOT_TIME_INFO,
    EXTRACT_JVM_DTO,
    BOOTING_SUT,
    /**
     * Request the agent's coverage-objective update count as a {@link Long}. The count
     * increases when an objective is first encountered or its best value improves, and is
     * reset by {@link org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer#reset()}.
     * The controller polls it to detect stable coverage while waiting for an AsyncAPI action
     * to finish; it counts updates, not distinct objectives or executions of covered code.
     */
    OBJECTIVE_UPDATE_COUNT
}
