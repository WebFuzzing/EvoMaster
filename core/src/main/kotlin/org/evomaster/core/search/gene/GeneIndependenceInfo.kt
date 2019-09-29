package org.evomaster.core.search.gene

import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator

/**
 * created by manzh on 2019-09-27
 */
class GeneIndependenceInfo(
        var degreeOfIndependence : Double = 0.0,
        var mutatedtimes : Int = 0,
        var resetTimes: Int = 0
){
    fun copy() : GeneIndependenceInfo{
        return GeneIndependenceInfo(degreeOfIndependence, mutatedtimes, resetTimes)
    }
}