package org.evomaster.core.search.service.mutator.genemutation

import org.evomaster.core.Lazy
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.GeneMutationSelectionMethod
import org.evomaster.core.search.service.mutator.EvaluatedMutation

/**
 * created by manzh on 2020-05-25
 *
 * the class contains additional info for gene mutation, e.g., archive-based gene mutation
 *   @property evi the evaluated individual contains an evolution of the gene with fitness values
 *   @property selection how to select genes to mutate if the gen contains more than one genes(e.g., ObjectGene) or other characteristics(e.g., size of ArrayGene)
 *   @property impact info of impact of the gene if it has, but in some case impact might be null, e.g., an element at ArrayGene
 *   @property geneReference a reference (i.e., id generated) to find a gene in this history, which always refers to 'root' gene in the [evi]
 *   @property archiveGeneSelector mutate genes using archive-based methods if the method is enabled or supports this type of the gene.
 *   @property targets are related to the mutation
 *   @property fromInitialization whether the gene is from InitializationAction
 *   @property position indicates an index of an action that contains the gene
 */

data class AdditionalGeneMutationInfo (
        val selection: GeneMutationSelectionMethod,
        val impact: GeneImpact?, //null is only allowed when the gene is root.
        val geneReference: String?, // null is only allowed when selection is NONE
        val archiveGeneSelector: ArchiveImpactSelector,
        val archiveGeneMutator: ArchiveGeneMutator,
        val evi: EvaluatedIndividual<*>,
        val targets: Set<Int>,
        val fromInitialization : Boolean = false,
        val position : Int = -1,
        val localId : String?,
        val rootGene : Gene,
        val effectiveHistory: MutableList<Gene> = mutableListOf(),
        val history: MutableList<Pair<Gene, EvaluatedInfo>> = mutableListOf()
){
    fun copyFoInnerGene(impact: GeneImpact?): AdditionalGeneMutationInfo{

        return AdditionalGeneMutationInfo(
                selection,
                impact, geneReference, archiveGeneSelector, archiveGeneMutator, evi, targets, rootGene = rootGene, localId = localId,
                effectiveHistory = effectiveHistory, history = history
        )
    }

    fun copyFoInnerGene(impact: GeneImpact?, gene : Gene): AdditionalGeneMutationInfo{
        Lazy.assert {
            effectiveHistory.size == this.effectiveHistory.size &&
            history.size == this.history.size
        }
        return AdditionalGeneMutationInfo(
                selection,
                impact, geneReference, archiveGeneSelector, archiveGeneMutator, evi, targets, rootGene = rootGene, localId = localId,
                effectiveHistory = effectiveHistory.mapNotNull {
                   it.getViewOfChildren().find { g-> g.possiblySame(gene) } }.toMutableList(),
                history = history.filter { it.first.getViewOfChildren().any { g-> g.possiblySame(gene) }}.map {
                    it.first.getViewOfChildren().find { g-> g.possiblySame(gene) }!! to it.second
                }.toMutableList()
        )
    }

    fun emptyHistory() = effectiveHistory.isEmpty() && history.isEmpty()

    //fun hasImpactInfo() : Boolean = impact != null

    fun hasHistory() : Boolean = history.any { targets.isEmpty() || (it.second.targets.any { t -> targets.contains(t) } && it.second.result?.isImpactful() ?: true) }
}

/**
 * a history for a gene
 */
data class EvaluatedInfo(
        val index : Int,
        val result : EvaluatedMutation?,
        val targets: Set<Int>,
        val specificTargets: Set<Int>
)