package org.evomaster.core.search.impact

/**
 * created by manzh on 2020-06-06
 */
interface CollectionImpact {

    fun recentImprovementOnSize() : Boolean = ImpactUtils.recentImprovement(getSizeImpact())

    fun getSizeImpact() : Impact
}