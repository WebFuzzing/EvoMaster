package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate


class IntegerGene(
        name: String,
        value: Int = 0,
        /** Inclusive */
        val min: Int = Int.MIN_VALUE,
        /** Inclusive */
        val max: Int = Int.MAX_VALUE,
        val valueMutation :IntMutationUpdate = IntMutationUpdate(min, max)
) : NumberGene<Int>(name, value) {

    override fun copy(): Gene {
        return IntegerGene(name, value, min, max, valueMutation.copy())
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is IntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is IntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val z = 1000
        val range = max.toLong() - min.toLong() + 1L

        val a: Int
        val b: Int

        if (range > z && randomness.nextBoolean(0.95)) {
            //if very large range, might want to sample small values around 0 most of the times
            if (min <= 0 && max >= z) {
                a = 0
                b = z
            } else if (randomness.nextBoolean()) {
                a = min
                b = min + z
            } else {
                a = max - z
                b = max
            }
        } else {
            a = min
            b = max
        }

        value = if (forceNewValue) {
            randomness.nextInt(a, b, value)
        } else {
            randomness.nextInt(a, b)
        }

    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        //check maximum range. no point in having a delta greater than such range
        val range: Long = max.toLong() - min.toLong()

        //choose an i for 2^i modification
        val delta = getDelta(randomness, apc, range)

        val sign = when (value) {
            max -> -1
            min -> +1
            else -> randomness.choose(listOf(-1, +1))
        }

        val res: Long = (value.toLong()) + (sign * delta)

        value = when {
            res > max -> max
            res < min -> min
            else -> res.toInt()
        }
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        standardMutation(randomness, apc, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return value.toString()
    }

    override fun reachOptimal(): Boolean {
        return valueMutation.reached
    }
}