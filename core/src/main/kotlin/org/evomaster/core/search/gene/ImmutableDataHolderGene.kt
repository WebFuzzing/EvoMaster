package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator


/**
 *  A gene representing existing data that cannot be modified,
 *  nor be directly used in a test action.
 *  However, it can be indirectly referred to.
 *
 *  A typical example is a Primary Key in a database, and we want
 *  a Foreign Key pointing to it
 */
class ImmutableDataHolderGene(name: String, val value: String, val inQuotes: Boolean) : Gene(name){

    override fun copy(): Gene {
        return this // recall it is immutable
    }

    override fun isMutable() = false

    override fun isPrintable() = true


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        throw IllegalStateException("Not supposed to modify an immutable gene")
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        throw IllegalStateException("Not supposed to modify an immutable gene")
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        standardMutation(randomness, apc, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {

        if(inQuotes){
            return "\"$value\""
        }
        return value
    }

    override fun copyValueFrom(other: Gene) {
        throw IllegalStateException("Not supposed to modify an immutable gene")
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is ImmutableDataHolderGene){
            return false
        }
        return value == other.value
    }
}