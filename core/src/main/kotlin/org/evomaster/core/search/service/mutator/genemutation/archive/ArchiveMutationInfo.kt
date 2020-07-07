package org.evomaster.core.search.service.mutator.genemutation.archive

import org.evomaster.core.search.gene.GeneIndependenceInfo

/**
 * created by manzh on 2020-06-10
 */


abstract class  ArchiveMutationInfo(
        /**
         * collect info regarding whether [this] gene is related to others
         */
        val dependencyInfo: GeneIndependenceInfo
): Comparable<ArchiveMutationInfo>{

    abstract fun copy() : ArchiveMutationInfo

    abstract fun reachOptimal() : Boolean

    fun plusMutatedTimes() {
        dependencyInfo.mutatedtimes += 1
    }

    fun plusResetTimes(){
        dependencyInfo.resetTimes += 1
    }

    fun doesDependOnOthers() : Boolean = dependencyInfo.resetTimes >= 2

    fun setDependencyDegree( degree : Double ) {
        dependencyInfo.degreeOfIndependence = degree
    }

    fun setDefaultLikelyDependent() {
        setDependencyDegree(0.8)
    }

    fun plusDependencyInfo() {
        plusResetTimes()
        if (doesDependOnOthers()) setDefaultLikelyDependent()
    }
}





