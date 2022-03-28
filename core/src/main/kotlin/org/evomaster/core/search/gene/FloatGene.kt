package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.DifferentGeneInHistory
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory



class FloatGene(name: String,
                value: Float = 0.0f,
                /** Inclusive */
                min : Float? = null,
                /** Inclusive */
                max : Float? = null,
                /**
                 * specified precision
                 */
                precision: Int? = null
) : FloatingPointNumber<Float>(name, value, min, max, precision) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FloatGene::class.java)
    }

    override fun copyContent() = FloatGene(name, value, min, max, precision)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        var rand = randomness.nextFloat()
        if (isRangeSpecified() && ((rand < (min ?: Float.MIN_VALUE)) || (rand > (max ?: Float.MAX_VALUE)))){
            rand = randomness.nextDouble((min?:Float.MIN_VALUE).toDouble(), (max?:Float.MAX_VALUE).toDouble()).toFloat()
        }
        value = getFormattedValue(rand)

    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if (enableAdaptiveGeneMutation){
            additionalGeneMutationInfo?:throw IllegalArgumentException("additional gene mutation info should not be null when adaptive gene mutation is enabled")
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


        value = mutateFloatingPointNumber(randomness, apc)

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return getFormattedValue().toString()
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


    override fun bindValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is FloatGene -> value = gene.value
            is DoubleGene -> value = gene.value.toFloat()
            is IntegerGene -> value = gene.value.toFloat()
            is LongGene -> value = gene.value.toFloat()
            is StringGene -> {
                value = gene.value.toFloatOrNull() ?: return false
            }
            is Base64StringGene ->{
                value = gene.data.value.toFloatOrNull() ?: return false
            }
            is ImmutableDataHolderGene -> {
                value = gene.value.toFloatOrNull() ?: return false
            }
            is SqlPrimaryKeyGene ->{
                value = gene.uniqueId.toFloat()
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "Do not support to bind float gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }

    override fun getMinimum(): Float {
        return min?:Float.MIN_VALUE
    }

    override fun getMaximum(): Float {
        return max?: Float.MAX_VALUE
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is FloatGene) {
            throw ClassCastException("Cannot compare FloatGene to ${other::javaClass} instance")
        }
        return this.toFloat().compareTo(other.toFloat())
    }


}