package org.evomaster.core.search.service.mutator.geneMutation

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.impactInfoCollection.GeneImpact
import org.evomaster.core.search.impact.impactInfoCollection.GeneMutationSelectionMethod

/**
 * created by manzh on 2020-05-25
 *
 * the class contains additional info for gene mutation, e.g., archive-based gene mutation
 *   @property evi the evaluated individual contains an evolution of the gene with fitness values
 *   @property selection how to select genes to mutate if the gen contains more than one genes(e.g., ObjectGene) or other characteristics(e.g., size of ArrayGene)
 *   @property impact info of impact of the gene if it has, but in some case impact might be null, e.g., an element at ArrayGene
 *   @property geneReference a reference (i.e., id generated) to find a gene in this history, which always refers to 'root' gene in the [evi]
 *   @property archiveMutator mutate genes using archive-based methods if the method is enabled or supports this type of the gene.
 *   @property targets are related to the mutation
 */

data class AdditionalGeneSelectionInfo (
        val selection: GeneMutationSelectionMethod,
        val impact: GeneImpact?, //null is only allowed when the gene is root.
        val geneReference: String?, // null is only allowed when selection is NONE
        val archiveMutator: ArchiveMutator,
        val evi: EvaluatedIndividual<*>,
        val targets: Set<Int>
){
    fun copyFoInnerGene(impact: GeneImpact?): AdditionalGeneSelectionInfo{
        return AdditionalGeneSelectionInfo(selection, impact, geneReference, archiveMutator, evi, targets)
    }
}