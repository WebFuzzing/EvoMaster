package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.DifferentGeneInHistory
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class IntegerGene(
        name: String,
        value: Int = 0,
        /** Inclusive */
        val min: Int = Int.MIN_VALUE,
        /** Inclusive */
        val max: Int = Int.MAX_VALUE
) : NumberGene<Int>(name, value) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(IntegerGene::class.java)
    }

    override fun copyContent(): Gene {
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

        value = randomness.randomizeBoundedIntAndLong(value.toLong(), min.toLong(), max.toLong(), forceNewValue).toInt()
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

    override fun bindValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is IntegerGene -> value = gene.value
            is FloatGene -> value = gene.value.toInt()
            is DoubleGene -> value = gene.value.toInt()
            is LongGene -> value = gene.value.toInt()
            is StringGene -> {
                value = gene.value.toIntOrNull() ?: return false
            }
            is Base64StringGene ->{
                value = gene.data.value.toIntOrNull() ?: return false
            }
            is ImmutableDataHolderGene -> {
                value = gene.value.toIntOrNull() ?: return false
            }
            is SqlPrimaryKeyGene ->{
                value = gene.uniqueId.toInt()
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind Integer with ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }
}