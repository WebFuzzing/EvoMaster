package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


class Base64StringGene(
        name: String,
        val data: StringGene = StringGene("data")
) : Gene(name) {

    companion object{
        val log : Logger = LoggerFactory.getLogger(Base64StringGene::class.java)
    }

    init {
        data.parent = this
    }

    override fun copy(): Gene = Base64StringGene(name, data.copy() as StringGene)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        data.randomize(randomness, forceNewValue)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        data.standardMutation(randomness, apc, allGenes)
    }

    override fun reachOptimal(): Boolean {
        return data.reachOptimal()
    }
    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is Base64StringGene){
                log.warn("original ({}) should be Base64StringGene", original::class.java.simpleName)
                return
            }
            if (mutated !is Base64StringGene){
                log.warn("mutated ({}) should be Base64StringGene", mutated::class.java.simpleName)
                return
            }
            data.archiveMutationUpdate(original.data, mutated.data, doesCurrentBetter, archiveMutator)
        }
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        if (!archiveMutator.enableArchiveMutation()){
            standardMutation(randomness, apc, allGenes)
            return
        }
        data.archiveMutation(randomness, allGenes, apc, selection, null, geneReference, archiveMutator, evi, targets)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return Base64.getEncoder().encodeToString(data.value.toByteArray())
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is Base64StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.data.copyValueFrom(other.data)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is Base64StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.data.containsSameValueAs(other.data)
    }


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if(excludePredicate(this)) listOf(this) else listOf(this).plus(data.flatView(excludePredicate))
    }
}