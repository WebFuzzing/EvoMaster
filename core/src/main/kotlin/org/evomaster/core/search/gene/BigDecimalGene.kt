package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.NumberMutator.randomizeDouble
import org.evomaster.core.search.gene.NumberMutator.randomizeLong
import org.evomaster.core.search.service.Randomness
import java.math.BigDecimal


class BigDecimalGene(
    name: String,
    value: BigDecimal = BigDecimal.ZERO,
    /** Inclusive */
    min : BigDecimal? = null,
    /** Inclusive */
    max : BigDecimal? = null,

    /**
     * indicate whether to employ float pointing
     * to mutate the gene
     */
    var floatingPointMode : Boolean,

    val precision : Int?,

    val scale : Int?

) : NumberGene<BigDecimal>(name, value, min, max){

    init {
        if (scale!= null && scale > 0){
            /*
                if [scale] is specified and it is greater than 0,
                floating mode is applied and being immutable
             */
            floatingPointMode = true
            forbidFloatingPointModeMutable()
        }
    }

    /**
     * whether to allow search to change [floatingPointMode]
     */
    var isFloatingPointMutable : Boolean = true
        private set

    fun forbidFloatingPointModeMutable(){
        isFloatingPointMutable = false
    }

    override fun copyContent(): BigDecimalGene{
        val copy = BigDecimalGene(name, value, min, max, floatingPointMode, precision, scale)
        copy.isFloatingPointMutable = this.isFloatingPointMutable
        return copy
    }

    override fun compareTo(other: ComparableGene): Int {
        TODO("Not yet implemented")
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        if (isFloatingPointMutable){
            if (forceNewValue){
                floatingPointMode = !floatingPointMode
                return
            } else if (randomness.nextBoolean())
                floatingPointMode = randomness.nextBoolean()
        }

        if (floatingPointMode){
            val dValue = randomizeDouble(getMinUsedInSearch(), getMaxUsedInSearch(), scale, randomness)
            setValueWithDouble(dValue)
        }else{
            val longValue = randomizeLong(value.toLong(), getMinUsedInSearch().toLong(), getMaxUsedInSearch().toLong(), randomness, forceNewValue)
            setValueWithLong(longValue)
        }
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
        TODO("Not yet implemented")
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        TODO("Not yet implemented")
    }

    override fun innerGene(): List<Gene> {
        TODO("Not yet implemented")
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        TODO("Not yet implemented")
    }


    fun setValueWithDouble(doubleValue: Double){
        TODO()
    }

    fun setValueWithLong(longValue: Long){
        TODO()
    }

    private fun getMinUsedInSearch() : Double {
        if (min == null || BigDecimal.valueOf(Double.MIN_VALUE) >= min) return Double.MIN_VALUE
        if (min >= BigDecimal.valueOf(Double.MAX_VALUE))
            throw IllegalStateException("not support yet: minimum value is greater than Double.MAX")
        return min.toDouble()
    }

    private fun getMaxUsedInSearch() : Double {
        if (max == null || BigDecimal.valueOf(Double.MAX_VALUE) <= min) return Double.MAX_VALUE
        if (max <= BigDecimal.valueOf(Double.MIN_VALUE))
            throw IllegalStateException("not support yet: max value is less than Double.MIN")
        return max.toDouble()
    }

    override fun getMinimum(): BigDecimal {
        return BigDecimal.valueOf(getMinUsedInSearch())
    }

    override fun getMaximum(): BigDecimal {
        return BigDecimal.valueOf(getMaxUsedInSearch())
    }
}