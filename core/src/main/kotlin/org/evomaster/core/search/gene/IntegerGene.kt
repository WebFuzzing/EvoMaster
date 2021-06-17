package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.DifferentGeneInHistory
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy


class IntegerGene(
        name: String,
        value: Int = 0,
        /** Inclusive */
        val min: Int = Int.MIN_VALUE,
        /** Inclusive */
        val max: Int = Int.MAX_VALUE
) : NumberGene<Int>(name, value) {

    override fun copy(): Gene {
        return IntegerGene(name, value, min, max)
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

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {

        if (enableAdaptiveGeneMutation){
            additionalGeneMutationInfo?:throw IllegalArgumentException("additional gene mutation info shouldnot be null when adaptive gene mutation is enabled")
            if (additionalGeneMutationInfo.hasHistory()){
                try {
                    additionalGeneMutationInfo.archiveGeneMutator.historyBasedValueMutation(
                            additionalGeneMutationInfo,
                            this,
                            allGenes
                    )
                    return true
                }catch (e: DifferentGeneInHistory){}
            }
        }

        //check maximum range. no point in having a delta greater than such range
        val range = max.toLong() - min.toLong()

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

        return true
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return value.toString()
    }

    override fun innerGene(): List<Gene> = listOf()

}