package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger

/**
 * gene represents bigInteger
 *
 * note that
 * currently the mutation of biginteger is limited to long mutation
 * however, biginteger could be mutated with `signum` and `mag`
 */
class BigIntegerGene(
    name: String,
    value: BigInteger = BigInteger.ZERO,
    /** Inclusive */
    min : BigInteger? = null,
    /** Inclusive */
    max : BigInteger? = null

) : NumberGene<BigInteger> (name, value, min, max) {


    companion object{
        private val log : Logger = LoggerFactory.getLogger(BigIntegerGene::class.java)

    }

    override fun copyContent(): BigIntegerGene = BigIntegerGene(name, value, min, max)


    override fun compareTo(other: ComparableGene): Int {
        if (other !is BigIntegerGene) {
            throw RuntimeException("Expected BigIntegerGene. Cannot compare to ${other::javaClass} instance")
        }
        return value.compareTo(other.value)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val longValue = NumberMutator.randomizeLong(value.toLong(), getMinUsedInSearch(), getMaxUsedInSearch(), randomness, forceNewValue)
        setValueWithLong(longValue)
    }

    override fun mutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        allGenes: List<Gene>,
        selectionStrategy: SubsetGeneSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        val mutated = super.mutate(randomness, apc, mwc, allGenes, selectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo)
        if (mutated) return true

        val longValue = NumberMutator.mutateLong(value.toLong(), getMinUsedInSearch(), getMaxUsedInSearch(), randomness, apc)
        setValueWithLong(longValue)
        return true
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is BigIntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        //BigInteger is immutable, just refer to the value of other gene
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is BigIntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value.compareTo(other.value) == 0
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        when(gene){
            is LongGene -> setValueWithLong(gene.value)
            is FloatGene -> setValueWithLong(gene.value.toLong())
            is IntegerGene -> setValueWithLong(gene.value.toLong())
            is DoubleGene -> setValueWithLong(gene.value.toLong())
            /*
                Man: TODO, it might need to handle radix for string, base64String
             */
            is StringGene -> gene.value.toBigIntegerOrNull()?: return false
            is Base64StringGene -> gene.data.value.toBigIntegerOrNull()?: return false
            is ImmutableDataHolderGene -> gene.value.toBigIntegerOrNull()?: return false
            is SqlPrimaryKeyGene -> setValueWithLong(gene.uniqueId)
            is BigDecimalGene -> setValueWithDecimal(gene.value)
            else -> {
                log.info("Do not support to bind long gene with the type: ${gene::class.java.simpleName}")
                return false
            }
        }

        return true
    }

    fun setValueWithDecimal(decimal: BigDecimal){
        value = decimal.toBigInteger()
    }

    fun setValueWithString(str: String, radix: Int = 10){
        value = BigInteger(str, radix)
    }

    fun setValueWithLong(longValue: Long) {
        value = BigInteger.valueOf(longValue)
    }

    override fun isValid(): Boolean {
        if (max != null && value <= max)
            return false
        if (min != null && value >= min)
            return false
        return true
    }

    /**
     * note that for biginteger
     * if the value exceeds the range manipulated by search, we consider it immutable for the moment
     */
    override fun isMutable(): Boolean {
        return super.isMutable() && checkValueRangeInSearch()
    }

    private fun checkValueRangeInSearch() : Boolean{
        val r = value <= BigInteger.valueOf(Long.MAX_VALUE) &&
                value >= BigInteger.valueOf(Long.MIN_VALUE)
        if (!r)
            LoggingUtil.uniqueWarn(log, "value of BigInteger exceeds the range of mutation")
        return r
    }

    private fun getMinUsedInSearch() : Long {
        if (min == null || BigInteger.valueOf(Long.MIN_VALUE) >= min) return Long.MIN_VALUE
        if (min >= BigInteger.valueOf(Long.MAX_VALUE))
            throw IllegalStateException("not support yet: minimum value is greater than Long.MAX")
        return min.toLong()
    }

    private fun getMaxUsedInSearch() : Long {
        if (max == null || BigInteger.valueOf(Long.MAX_VALUE) <= min) return Long.MAX_VALUE
        if (max <= BigInteger.valueOf(Long.MIN_VALUE))
            throw IllegalStateException("not support yet: max value is less than Long.MIN")
        return max.toLong()
    }

    override fun getMinimum(): BigInteger {
        return BigInteger.valueOf(getMinUsedInSearch())
    }

    override fun getMaximum(): BigInteger {
        return BigInteger.valueOf(getMaxUsedInSearch())
    }

}