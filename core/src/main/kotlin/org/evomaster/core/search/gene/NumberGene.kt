package org.evomaster.core.search.gene

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.min

/**
 * Common superclass for all number genes (i.e. Float,Double,Integer,Long)
 */
abstract class NumberGene<T : Number>(name: String,
                                      var value: T,
                                      /**
                                       * lower bound of the number
                                       */
                                      open val min : T?,
                                      /**
                                       * upper bound of the number
                                       */
                                      open val max : T?,
                                      /**
                                       * indicate whether to include the lower bound
                                       */
                                      val minInclusive : Boolean,
                                      /**
                                       * indicate whether to include the upper bound
                                       */
                                      val maxInclusive : Boolean
                                      ) : ComparableGene(name, mutableListOf()) {

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
        if (max != null && max !is BigDecimal && max !is BigInteger && value.toDouble() > max!!.toDouble())
            return false
        if (min != null && max !is BigDecimal && max !is BigInteger && value.toDouble() < min!!.toDouble())
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

}