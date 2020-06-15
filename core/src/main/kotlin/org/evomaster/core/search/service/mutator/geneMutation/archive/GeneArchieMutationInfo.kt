package org.evomaster.core.search.service.mutator.geneMutation.archive

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.StringGene

/**
 * This class contains archive-based info for a gene
 * For different associated targets, the gene might need to reach different values.
 * [map] is used to restore such values with respect to the targets.
 *
 * when the gene is mutated at first time, the associated targets are unknown.
 * in this case, [initialMap] is used to restore the mutation info tagged with current evaluatedIndividuals as a key for the mutation.
 * after the mutation is evaluated, the mutation info will be associated with the targets.
 */
class GeneArchieMutationInfo(

        /**
         * key is target
         * value is collected archive mutation info
         */
        val map : MutableMap<Int, ArchiveMutationInfo> = mutableMapOf()
){

    fun copy() : GeneArchieMutationInfo{
        return GeneArchieMutationInfo(map.map { it.key to it.value.copy() }.toMap().toMutableMap())
    }

    fun getArchiveMutationInfo(gene : Gene, target: Int) : ArchiveMutationInfo?{
        return map.getOrPut(target,
                {when(gene){
                    is StringGene -> StringGeneArchiveMutationInfo(gene)
                    is IntegerGene -> IntegerGeneArchiveMutationInfo(minValue = gene.min, maxValue = gene.max)
                    else ->{
                        TODO()
                    }
                }}
        )
    }

    fun sort(targets: Set<Int>) : List<ArchiveMutationInfo>{
        return when{
            map.values.all { it is StringGeneArchiveMutationInfo } ->{
                map.filterKeys { targets.contains(it) }.values.sortedBy {  it as StringGeneArchiveMutationInfo }
            }
            else -> {
                TODO()
            }
        }
    }

    fun reachOptimal(targets : Set<Int>) = map.filterKeys { targets.contains(it) }.all { it.value.reachOptimal() }
}