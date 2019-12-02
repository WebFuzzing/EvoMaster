package org.evomaster.core.search.impact

/**
 * created by manzh on 2019-09-13
 *
 * [archive] indicates whether the method is archive-based method
 * [adaptive] indicates whether the method is adaptive
 */
enum class GeneMutationSelectionMethod(val archive: Boolean = true, val adaptive: Boolean = false) {
    NONE(archive = false),
    /**
     * penalty function
     */
    AWAY_NOIMPACT,
    /**
     * reward function
     */
    APPROACH_IMPACT,
    /**
     * reward function with a consideration of latest
     */
    APPROACH_LATEST_IMPACT,
    /**
     * reward function with a consideration of latest
     */
    APPROACH_LATEST_IMPROVEMENT,
    /**
     * penalty and reward function
     */
    BALANCE_IMPACT_NOIMPACT,
    /**
     * randomly apply one of fixed archive-based gene selection method ([archive] is true and [adaptive] is false)
     */
    ALL_FIXED_RAND(adaptive = true)
}