package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator

/**
 * It might happen that object A has reference to B,
 * and B has reference to A', where A' might or might
 * not be equal to A.
 * In this case, we cannot represent A'.
 *
 * TODO need to handle cases when some of those are
 * marked with "required"
 *
 * Note that for [CycleObjectGene], its [refType] is null
 */
class CycleObjectGene(name: String) : ObjectGene(name, listOf()) {

    override fun isMutable() = false

    override fun copy(): Gene = CycleObjectGene(name)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        //nothing to do
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        randomize(randomness, true, allGenes)
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        //nothing to do?
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        //nothing to do?
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        throw IllegalStateException("CycleObjectGene has no value")
    }

}