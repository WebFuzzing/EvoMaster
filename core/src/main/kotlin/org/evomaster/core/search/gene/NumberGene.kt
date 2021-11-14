package org.evomaster.core.search.gene

import org.evomaster.core.problem.rest.NumericConstrains

import kotlin.math.min


/**
 * Common superclass for all number genes (i.e. Float,Double,Integer,Long)
 */
abstract class NumberGene<T : Number>(name: String, var value: T, var numericConstrains: NumericConstrains? = null) : Gene(name, mutableListOf()) {


    var min: Number? = numericConstrains?.getMin()
    var max: Number? = numericConstrains?.getMax()
    var exclusiveMinimum: Boolean = numericConstrains?.getExclusiveMinimum() ?: false
    var exclusiveMaximum: Boolean = numericConstrains?.getExclusiveMaximum() ?: false

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
        if (max != null){
            if (!exclusiveMaximum && value.toDouble() > max!!.toDouble())
                return false
            else if (exclusiveMaximum && value.toDouble() >= max!!.toDouble())
                return false
        }
        if (min != null) {
            if (!exclusiveMinimum && value.toDouble() < min!!.toDouble())
                return false
            else if (!exclusiveMinimum && value.toDouble() <= min!!.toDouble())
                return false
        }
        return true
    }

}