package org.evomaster.core.search.gene

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.min

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
                                      ) : ComparableGene, SimpleGene(name) {


    var value : T

    init {
        if (value == null)
            this.value = getDefaultValue()
        else
            this.value = value
    }


    override fun getChildren(): MutableList<Gene> = mutableListOf()

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
    override fun isValid() : Boolean{
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

}