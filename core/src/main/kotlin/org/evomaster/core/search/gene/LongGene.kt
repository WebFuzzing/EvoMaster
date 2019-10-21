package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness


class LongGene(
        name: String,
        value: Long = 0
) : NumberGene<Long>(name, value) {


    override fun copy(): Gene {
        val copy = LongGene(name, value)
        return copy
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        var k = if (randomness.nextBoolean(0.1)) {
            randomness.nextLong()
        } else {
            randomness.nextInt().toLong()
        }

        while (forceNewValue && k == value) {
            k = randomness.nextInt().toLong()
        }

        value = k
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        //choose an i for 2^i modification
        val delta = GeneUtils.getDelta(randomness, apc)

        val sign = randomness.choose(listOf(-1, +1))

        value += (sign * delta)
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        standardMutation(randomness, apc, allGenes)
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is LongGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is LongGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

}