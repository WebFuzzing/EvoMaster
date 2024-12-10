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
    val searchTime: Int,

    /**
     * achieved by seeded tests
     */
    val seedingTime: Int,

    /**
     * total unique targets covered at the end
     * this includes targets covered during boot-time, search-time and authentication
     * since there might exist duplicated targets in different phases, then set unique amount
     * with this property
     */
    val total : Int
){
    /**
//     * derived based on [bootTime] and [searchTime]
//     */
//    val total: Int
//        get(){return bootTime + searchTime}
}
