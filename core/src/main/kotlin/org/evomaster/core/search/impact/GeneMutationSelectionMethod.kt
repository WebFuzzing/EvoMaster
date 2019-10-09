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
     * avoid a percentage (specified in [EMConfig.perOfCandidateGenesToMutate]) of genes which have lower degree of impacts
     */
    AWAY_NOIMPACT,
    /**
     * select a percentage (specified in [EMConfig.perOfCandidateGenesToMutate]) of genes which have lower degree of no impacts
     */
    //APPROACH_IMPACT_N,
    /**
     * select a percentage (specified in [EMConfig.perOfCandidateGenesToMutate]) of genes which have higher impacts
     */
    APPROACH_IMPACT_I,

    //FEED_DIRECT_IMPACT,

    /**
     * randomly apply one of fixed archive-based gene selection method ([archive] is true and [adaptive] is false)
     */
    ALL_FIXED_RAND(true)
}