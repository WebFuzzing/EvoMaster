package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import java.math.BigDecimal
import java.math.RoundingMode


class FloatGene(name: String,
                value: Float = 0.0f
) : NumberGene<Float>(name, value) {

    override fun copy() = FloatGene(name, value)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        //need for forceNewValue?
        value = randomness.nextFloat()
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if (enableAdaptiveGeneMutation){
            additionalGeneMutationInfo?:throw IllegalArgumentException("additional gene mutation info shouldnot be null when adaptive gene mutation is enabled")
            if (additionalGeneMutationInfo.hasHistory()){
                additionalGeneMutationInfo.archiveGeneMutator.historyBasedValueMutation(
                        additionalGeneMutationInfo,
                        this,
                        allGenes
                )
                return true
            }
        }

        //TODO min/max for Float
        val k = when (randomness.choose(listOf(0, 1, 2))) {
            //for small changes
            0 -> value + randomness.nextGaussian()
            //for large jumps
            1 -> value + (GeneUtils.getDelta(randomness, apc) * randomness.nextGaussian())
            //to reduce precision, ie chop off digits after the "."
            2 -> BigDecimal(value.toDouble()).setScale(randomness.nextInt(15), RoundingMode.HALF_EVEN).toDouble()
            else -> throw IllegalStateException("Regression bug")
        }

        value = k.toFloat()

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is FloatGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is FloatGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }
    override fun innerGene(): List<Gene> = listOf()

}