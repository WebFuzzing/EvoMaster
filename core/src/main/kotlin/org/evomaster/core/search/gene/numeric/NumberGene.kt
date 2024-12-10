package org.evomaster.core.search.gene.numeric

import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.interfaces.HistoryBasedMutationGene
import org.evomaster.core.search.gene.root.SimpleGene
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Common superclass for all number genes (i.e. Float,Double,Integer,Long)
 */
abstract class NumberGene<T : Number>(name: String,
                                      value: T?,
                                      /**
                                       * lower bound of the number
                                       */
                                      val min : T?,
                                      /**
                                       * upper bound of the number
                                       */
                                      val max : T?,
                                      /**
                                       * indicate whether to include the lower bound
                                       */
                                      val minInclusive : Boolean,
                                      /**
                                       * indicate whether to include the upper bound
                                       */
                                      val maxInclusive : Boolean,
                                      /**
                                       * maximum number of digits
                                       *
                                       * Note that this presents the max range,
                                       * eg, DEC(4,2) on mysql, @Digits(integer=2, fraction=2)
                                       * the precision is 4 and the scale is 2
                                       * its range would be from -99.99 to 99.99.
                                       * 5.2 and 0.1 are considered as `valid`
                                       */
                                      val precision: Int?,
                                      /**
                                       * maximum number of digits to the right of the decimal point
                                       *
                                       * Note that this presents the max range,
                                       * eg, DEC(4,2) on mysql, @Digits(integer=2, fraction=2)
                                       * the precision is 4 and the scale is 2
                                       * its range would be from -99.99 to 99.99.
                                       * 5.2 and 0.1 are considered as `valid`
                                       */
                                      val scale: Int?
                                      ) : ComparableGene, HistoryBasedMutationGene, SimpleGene(name) {


    var value : T

    init {
        if (value == null)
            this.value = getDefaultValue()
        else
            this.value = value

        if (precision != null && precision <= 0)
            throw IllegalArgumentException("precision must be positive number")

        if (scale != null && scale < 0)
            throw IllegalArgumentException("scale must be zero or positive number")

        if (getMaximum().toDouble() < getMinimum().toDouble())
            throwMinMaxException()
    }

    fun throwMinMaxException(){
        val x = if(minInclusive) "inclusive" else "exclusive"
        val y = if(maxInclusive) "inclusive" else "exclusive"
        throw IllegalArgumentException("max must be greater than min but $y max is $max and $x min is $min")
    }

    open fun isRangeSpecified() = min != null || max != null

    fun toInt(): Int =
            value.toInt()


    fun toLong(): Long =
            value.toLong()


    fun toDouble(): Double =
            value.toDouble()


    fun toFloat(): Float =
            value.toFloat()


    /**
     * @return whether the [value] is between [min] and [max] if they are specified
     */
    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        if (max != null && max !is BigDecimal && max !is BigInteger && value.toDouble() > max.toDouble())
            return false
        if (min != null && max !is BigDecimal && max !is BigInteger && value.toDouble() < min.toDouble())
            return false
        return true
    }

    override fun isMutable(): Boolean {
        // it is not mutable if max equals to min
        return min == null || max == null || max != min
    }

    /**
     * @return inclusive Minimum value of the gene
     */
    abstract fun getMinimum() : T

    /**
     * @return inclusive Maximum value of the gene
     */
    abstract fun getMaximum() : T

    /**
     * @return a default value if the value is not specified
     */
    open fun getDefaultValue() : T = getZero()

    /**
     * @return zero with the number format
     */
    abstract fun getZero() : T

    override fun toString(): String {
        return "${this.javaClass.simpleName}: $value [$minInclusive $min, $maxInclusive $max] [s=$scale,p=$precision]"
    }
}