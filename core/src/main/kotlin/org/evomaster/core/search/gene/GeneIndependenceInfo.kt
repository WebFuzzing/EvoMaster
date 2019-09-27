package org.evomaster.core.search.gene

import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator

/**
 * created by manzh on 2019-09-27
 */
interface GeneIndependenceInfo {

    var degreeOfIndependence : Double
        get() = ArchiveMutator.WITHIN_NORMAL
        set(value) {
            degreeOfIndependence = value
        }

    var mutatedtimes : Int
        get() = 0
        set(value) {
            mutatedtimes = value
        }

    var resetTimes: Int
        get() = 0
        set(value) {
            resetTimes = value
        }
}