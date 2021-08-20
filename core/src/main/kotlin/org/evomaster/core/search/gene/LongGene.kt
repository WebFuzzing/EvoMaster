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
import kotlin.math.min


class LongGene(
        name: String,
        value: Long = 0,
        val min : Long? = null,
        val max : Long? = null
) : NumberGene<Long>(name, value) {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(LongGene::class.java)
    }

    override fun copyContent(): Gene {
        val copy = LongGene(name, value, min, max)
        return copy
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        /*
            if one of min or max is specified,
            we employ [randomness.randomizeBoundedIntAndLong] for randomizing long that is same as randomizing int
         */
        if (isRangeSpecified()){
            value = randomness.randomizeBoundedIntAndLong(value, min?: Long.MIN_VALUE, max?: Long.MAX_VALUE, forceNewValue)
            return
        }

        var k = if (randomness.nextBoolean(0.1)) {
                randomness.nextLong()
        } else if (randomness.nextBoolean(0.1)) {
            randomness.nextInt().toLong()
        } else {
            randomness.nextInt(1000).toLong()
        }

        while (forceNewValue && k == value) {
            k = randomness.nextInt().toLong()
        }

        value = k
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{
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

        //choose an i for 2^i modification
        val delta = GeneUtils.getDelta(randomness, apc, delta())

        val sign = when{
            max != null && value >= max -> -1
            min != null && value <= min -> +1
            else -> randomness.choose(listOf(-1, +1))
        }

        value += (sign * delta)


        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
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

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is LongGene -> value = gene.value
            is FloatGene -> value = gene.value.toLong()
            is IntegerGene -> value = gene.value.toLong()
            is DoubleGene -> value = gene.value.toLong()
            is StringGene -> {
                value = gene.value.toLongOrNull() ?: return false
            }
            is Base64StringGene ->{
                value = gene.data.value.toLongOrNull() ?: return false
            }
            is ImmutableDataHolderGene -> {
                value = gene.value.toLongOrNull() ?: return false
            }
            is SqlPrimaryKeyGene ->{
                value = gene.uniqueId
            }
            else -> {
                log.info("Do not support to bind long gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }

    private fun isRangeSpecified() = min != null || max != null


    /**
     * calculate the delta based on [min] and [max] that is used for eg, mutation
     * note the delta is less than [Long.MAX_VALUE]
     */
    private fun delta() : Long{
        return if (isRangeSpecified()){
            try{
                min(Long.MAX_VALUE, Math.subtractExact(max?: Long.MAX_VALUE, min?: Long.MIN_VALUE))
            }catch (e : ArithmeticException) {
                Long.MAX_VALUE
            }
        }else
            Long.MAX_VALUE

    }
}