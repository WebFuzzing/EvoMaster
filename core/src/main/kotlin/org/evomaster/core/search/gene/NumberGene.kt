package org.evomaster.core.search.gene

import kotlin.math.min

/**
 * Common superclass for all number genes (i.e. Float,Double,Integer,Long)
 */
abstract class NumberGene<T : Number>(name: String,
                                      var value: T,
                                      open val min : T?,
                                      open val max : T?) : ComparableGene(name, mutableListOf()) {

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
        if (max != null && value.toDouble() > max!!.toDouble())
            return false
        if (min != null && value.toDouble() < min!!.toDouble())
            return false
        return true
    }

}