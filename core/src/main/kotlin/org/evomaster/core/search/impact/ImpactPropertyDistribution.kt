package org.evomaster.core.search.impact

/**
 * created by manzh on 2019-10-15
 * regarding each individual, impact property distribution varies.
 * we further classify them as this [ImpactPropertyDistribution]
 */
enum class ImpactPropertyDistribution(val rank : Int){
    ALL(4),
    NONE(0),
    FEW(1),
    MOST(2),
    EQUAL(3)
}