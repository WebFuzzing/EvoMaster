package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.value.DisruptiveGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * A gene that has a major, disruptive impact on the whole chromosome.
 * As such, it should be mutated only with low probability
 */
class DisruptiveGene<out T>(name: String, val gene: T, var probability: Double) : Gene(name)
        where T : Gene {

    init {
        if (probability < 0 || probability > 1) {
            throw IllegalArgumentException("Invalid probability value: $probability")
        }
        if (gene is DisruptiveGene<*>) {
            throw IllegalArgumentException("Cannot have a recursive disruptive gene")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(DisruptiveGene::class.java)
    }

    init {
        gene.parent = this
    }


    override fun copy(): Gene {
        return DisruptiveGene(name, gene.copy(), probability)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        gene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        if(randomness.nextBoolean(probability)){
            gene.standardMutation(randomness, apc, allGenes)
        }
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        if (!archiveMutator.enableArchiveMutation()){
            standardMutation(randomness,apc, allGenes)
            return
        }

        gene.archiveMutation(randomness, allGenes, apc, selection, if(impact == null || impact !is DisruptiveGeneImpact) null else impact.geneImpact, geneReference, archiveMutator, evi,targets )
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun isMutable() = probability > 0

    override fun copyValueFrom(other: Gene) {
        if (other !is DisruptiveGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.gene.copyValueFrom(other.gene)
        this.probability = other.probability
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DisruptiveGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        /**
         * Not sure if probability is part of the
         * value for this gene
         */
        return this.gene.containsSameValueAs(other.gene)
                && this.probability == other.probability
    }


    override fun getVariableName() = gene.getVariableName()

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if(excludePredicate(this)) listOf(this) else listOf(this).plus(gene.flatView(excludePredicate))
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is DisruptiveGene<*>){
                log.warn("original ({}) should be DisruptiveGene", original::class.java.simpleName)
                return
            }
            if (mutated !is DisruptiveGene<*>){
                log.warn("mutated ({}) should be DisruptiveGene", mutated::class.java.simpleName)
                return
            }
            gene.archiveMutationUpdate(original.gene, mutated.gene, doesCurrentBetter, archiveMutator)
        }
    }
}