package org.evomaster.core.search.impact.impactinfocollection

/**
 * created by manzh on 2020-06-06
 */
interface CollectionImpact {

    fun recentImprovementOnSize() : Boolean = getSizeImpact().recentImprovement()

    fun getSizeImpact() : Impact
}