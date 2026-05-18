package org.evomaster.core.search.warning

enum class WarningCategory(
    /**
     * Hint on the priority of this warning message, where lowest (eg, 1) values mean
     * more important. This is ONLY meant to be used for display purposes, e.g., when
     * sorting the list of warnings to show to user.
     */
    val displayPriority: Int
) {

    /**
     * Issues related to the schema of the SUT
     */
    SCHEMA(3),

    /**
     * Issues related to how the fuzzer was configured (eg no auth configured)
     */
    FUZZER(1),

    /**
     * Issues related to what the SUT is returning or is configured (eg rate limiter is on)
     */
    SUT(2)
}