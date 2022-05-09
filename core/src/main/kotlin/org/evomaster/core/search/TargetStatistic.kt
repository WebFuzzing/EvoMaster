package org.evomaster.core.search

/**
 * info for targets covered during different phases
 */
data class TargetStatistic(
    /**
     * SUT boot-time
     *
     * negative means that the boot-time info is unavailable
     */
    val bootTime: Int,
    /**
     * achieved by search
     */
    val searchTime: Int
){
    /**
     * derived based on [bootTime] and [searchTime]
     */
    val total: Int
        get(){return bootTime + searchTime}
}
