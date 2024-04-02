package org.evomaster.core.search.gene.numeric

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.NumericStringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.evomaster.core.utils.NumberCalculationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

class IntegerGene(
    name: String,
    value: Int?,
    min: Int,
    max: Int,
    precision : Int?,
    minInclusive : Boolean,
    maxInclusive : Boolean,
) : IntegralNumberGene<Int>(name, value, min, max, precision, minInclusive, maxInclusive) {

    constructor(name: String, value: Int? = null, min: Int? = null, max: Int?=null, precision: Int?=null, minInclusive: Boolean = true, maxInclusive: Boolean = true) :this(
        name, value,
        min = (min?:Int.MIN_VALUE).run { if (precision!= null) max(
            this,
            (-NumberCalculationUtil.upperBound(precision, 0)).toInt()
        ) else this },
        max = (max?:Int.MAX_VALUE).run { if (precision!= null) min(
            this,
            (NumberCalculationUtil.upperBound(precision, 0)).toInt()
        ) else this },
        precision, minInclusive, maxInclusive)

    init {
        if (getMaximum() == getMinimum())
            this.value = getMinimum()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(IntegerGene::class.java)
    }

    override fun setFromStringValue(value: String) : Boolean{
        try{
            this.value = value.toInt()
            return true
        }catch (e: NumberFormatException){
            return false
        }
    }

    override fun copyContent(): Gene {
        return IntegerGene(
            name,
            value,
            precision = precision,
            min = min,
            max = max,
            minInclusive = minInclusive,
            maxInclusive = maxInclusive
        )
    }

    override fun setValueWithRawString(value: String) {
        try {
            this.value = value.toInt()
        }catch (e: Exception){
            LoggingUtil.uniqueWarn(log, "fail to set IntegerGene with a value which is not int format (ie, $value)")
        }

    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is IntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        val current = this.value
        this.value = other.value
        if (!isLocallyValid()){
            this.value = current
            return false
        }

        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is IntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        value = randomness.randomizeBoundedIntAndLong(value.toLong(), getMinimum().toLong(), getMaximum().toLong(), tryToForceNewValue).toInt()
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {

        if(enableAdaptiveGeneMutation){
            val mutated = applyHistoryBasedMutation(this, additionalGeneMutationInfo!!)
            if (mutated) return true
        }

        //check maximum range. no point in having a delta greater than such range
        val range = getMaximum().toLong() - getMinimum().toLong()

        //choose an i for 2^i modification
        val delta = GeneUtils.getDelta(randomness, apc, range)

        val sign = when (value) {
            max -> -1
            min -> +1
            else -> randomness.choose(listOf(-1, +1))
        }

        val res: Long = (value.toLong()) + (sign * delta)

        value = when {
            res > getMaximum() -> getMaximum()
            res < getMinimum() -> getMinimum()
            else -> res.toInt()
        }

        return true
    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val stringValue = value.toString()
        return if(mode==GeneUtils.EscapeMode.EJSON) "{\"\$numberInt\":\"$stringValue\"}" else stringValue
    }


    override fun bindValueBasedOn(gene: Gene): Boolean {
        when (gene) {
            is IntegerGene -> value = gene.value
            is FloatGene -> value = gene.value.toInt()
            is DoubleGene -> value = gene.value.toInt()
            is LongGene -> value = gene.value.toInt()
            is BigDecimalGene -> value = try { gene.value.toInt() } catch (e: Exception) { return false }
            is BigIntegerGene -> value = try { gene.value.toInt() } catch (e: Exception) { return false }
            is StringGene -> {
                value = gene.value.toIntOrNull() ?: return false
            }
            is Base64StringGene -> {
                value = gene.data.value.toIntOrNull() ?: return false
            }
            is ImmutableDataHolderGene -> {
                value = gene.value.toIntOrNull() ?: return false
            }
            is SqlPrimaryKeyGene -> {
                value = gene.uniqueId.toInt()
            }
            is SeededGene<*> ->{
                return this.bindValueBasedOn(gene.getPhenotype() as Gene)
            }
            is NumericStringGene ->{
                return this.bindValueBasedOn(gene.number)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind Integer with ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is IntegerGene) {
            throw ClassCastException("Expected IntegerGene but " + other::javaClass + " was found.")
        }
        return this.toInt().compareTo(other.toInt())
    }

    override fun isMutable(): Boolean {
        return this.max!! > this.min!!
    }

    override fun getMaximum(): Int = max!!.run { if (!maxInclusive) this - 1 else this }

    override fun getMinimum(): Int = min!!.run { if (!minInclusive) this + 1 else this }


    override fun getDefaultValue(): Int {
        val df = super.getDefaultValue()
        if (df <= getMaximum() && df >= getMinimum())
            return df
        return NumberCalculationUtil.getMiddle(getMinimum(), getMaximum(), 0).toInt()
    }

    override fun getZero(): Int = 0
}