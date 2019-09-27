package org.evomaster.core.search.impact

/**
 * created by manzh on 2019-09-13
 */
enum class ImpactMutationSelection {
    NONE,
    /**
     * avoid genes (only 10%) which have lower degree of impacts
     */
    AWAY_NOIMPACT,
    /**
     * select genes which have higher degree of impacts
     */
    APPROACH_IMPACT,
    /**
     * select genes which have recent impacts
     */
    //FEEDBACK_DIRECT
}