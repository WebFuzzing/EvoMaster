package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import java.math.BigDecimal
import java.math.RoundingMode


class DoubleGene(name: String,
                 value: Double = 0.0
) : NumberGene<Double>(name, value) {

    override fun copy() = DoubleGene(name, value)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        //need for forceNewValue?
        value = randomness.nextDouble()
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?) : Boolean{

        //TODO min/max for Double
        value = when (randomness.choose(listOf(0, 1, 2))) {
            //for small changes
            0 -> value + randomness.nextGaussian()
            //for large jumps
            1 -> value + (getDelta(randomness, apc) * randomness.nextGaussian())
            //to reduce precision, ie chop off digits after the "."
            2 -> BigDecimal(value).setScale(randomness.nextInt(15), RoundingMode.HALF_EVEN).toDouble()
            else -> throw IllegalStateException("Regression bug")
        }
        return true

        //TODO with archive-based mutation
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DoubleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DoubleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

}