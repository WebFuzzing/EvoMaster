package org.evomaster.core.search.gene

import org.evomaster.core.problem.rest.NumericConstrains

/**
 * Common superclass for all number genes (i.e. Float,Double,Integer,Long)
 */
abstract class NumberGene<T : Number>(name: String, var value: T, var numericConstrains: NumericConstrains? = null) : Gene(name, mutableListOf()) {

    override fun getChildren(): MutableList<Gene> = mutableListOf()

    fun toInt(): Int =
            value.toInt()


    fun toLong(): Long =
            value.toLong()


    fun toDouble(): Double =
            value.toDouble()


    fun toFloat(): Float =
            value.toFloat()


}