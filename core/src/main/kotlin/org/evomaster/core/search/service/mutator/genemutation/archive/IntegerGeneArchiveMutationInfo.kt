package org.evomaster.core.search.service.mutator.genemutation.archive

import org.evomaster.core.search.gene.GeneIndependenceInfo
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.mutator.genemutation.IntMutationUpdate

/**
 * created by manzh on 2020-06-12
 */

class IntegerGeneArchiveMutationInfo(
        val valueMutation : IntMutationUpdate,
        dependencyInfo: GeneIndependenceInfo = GeneIndependenceInfo()) : ArchiveMutationInfo(dependencyInfo){


    constructor(minValue : Int, maxValue : Int) : this(valueMutation = IntMutationUpdate(minValue, maxValue))
    constructor(gene: IntegerGene) : this(valueMutation = IntMutationUpdate(gene.min, gene.max))


    override fun copy(): IntegerGeneArchiveMutationInfo {
        return IntegerGeneArchiveMutationInfo(valueMutation.copy(), dependencyInfo.copy())
    }

    override fun reachOptimal(): Boolean {
        return valueMutation.reached
    }

    override fun compareTo(other: ArchiveMutationInfo): Int {
        if (other !is IntegerGeneArchiveMutationInfo)
            throw IllegalArgumentException("should compare with same type")

        return valueMutation.compareTo(other.valueMutation)
    }
}