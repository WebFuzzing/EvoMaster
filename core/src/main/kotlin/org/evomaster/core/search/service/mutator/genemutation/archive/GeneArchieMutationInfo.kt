package org.evomaster.core.search.service.mutator.genemutation.archive

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator

/**
 * This class contains archive-based info for a gene
 * For different targets, optimal value of the gene might be different.
 * [map] is used to restore such values with respect to the targets.
 * Besides, such map is shared among the individual by mutator.
 */
class GeneArchieMutationInfo(

        /**
         * key is target
         * value is collected archive mutation info
         */
        val map : MutableMap<Int, ArchiveMutationInfo> = mutableMapOf()
){

    fun clone() : GeneArchieMutationInfo{
        return this
    }

    fun copy() : GeneArchieMutationInfo = GeneArchieMutationInfo(map.map { it.key to it.value.copy() }.toMap().toMutableMap())

    fun getArchiveMutationInfo(gene : Gene, target: Int, archiveGeneMutator: ArchiveGeneMutator) : ArchiveMutationInfo?{
        return map.getOrPut(target,
                {when(gene){
                    is StringGene -> StringGeneArchiveMutationInfo(gene, archiveGeneMutator)
                    is IntegerGene -> IntegerGeneArchiveMutationInfo(minValue = gene.min, maxValue = gene.max)
                    else ->{
                        TODO()
                    }
                }}
        )
    }

    /**
     * To cover different targets, optimal value might be different,
     * thus, we sort GeneArchiveMutationInfo with given targets
     */
    fun sort(targets: Set<Int>) : List<ArchiveMutationInfo>{
        return when{
            map.values.all { it is StringGeneArchiveMutationInfo} || map.values.all { it is IntegerGeneArchiveMutationInfo } ->{
                map.filterKeys { targets.contains(it) }.values.sortedBy {  it }
            }
            else -> {
                TODO()
            }
        }
    }

    fun reachOptimal(targets : Set<Int>) = map.filterKeys { targets.contains(it) }.all { it.value.reachOptimal() }
}