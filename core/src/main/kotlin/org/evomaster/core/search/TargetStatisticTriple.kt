package org.evomaster.core.search

/**
 * info for targets covered during different phases
 */
data class TargetStatisticTriple(
    /**
     * SUT boot-time
     *
     * negative means that the boot-time info is unavailable
     */
    val bootTime: Int,
    /**
     * achieved by search
     */
    val searchTime: Int,
    /**
     * a total of unique targets covered at the end
     */
    val total: Int
)
