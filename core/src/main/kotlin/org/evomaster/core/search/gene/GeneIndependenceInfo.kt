package org.evomaster.core.search.gene

import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator

/**
 * used to collect gene independency degree
 * @property mutatedtimes mutation times of a gene
 * @property resetTimes times to reset an valid mutation boundary.
 * @property degreeOfIndependence a degree of gene independency that may be derived based on [mutatedtimes] and [resetTimes]
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