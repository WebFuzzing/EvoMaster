package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

/**
 * Common superclass for all number genes (i.e. Float,Double,Integer,Long)
 */
abstract class NumberGene<T : Number>(name: String,
                                      var value: T,
                                      open val min : T?,
                                      open val max : T?) : Gene(name, mutableListOf()) {

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


    fun modifyValue(randomness: Randomness, value: Double, delta: Double, maxRange: Long, specifiedJumpDelta: Int, precisionChangeable: Boolean): Double{
        val strategies = ModifyStrategy.values().filter{
            precisionChangeable || it != ModifyStrategy.REDUCE_PRECISION
        }
        return when(randomness.choose(strategies)){
            ModifyStrategy.SMALL_CHANGE-> value + min(1, maxRange) * delta
            ModifyStrategy.LARGE_JUMP -> value + specifiedJumpDelta * delta
            ModifyStrategy.REDUCE_PRECISION -> BigDecimal(value).setScale(randomness.nextInt(15), RoundingMode.HALF_EVEN).toDouble()
        }

    }

    /**
     * @return formatted value
     */
    open fun getFormattedValue(valueToFormat: T? = null) = valueToFormat?:value

    /**
     * @return minimal delta if it has
     */
    open fun getMinimalDelta() : T? = null

    enum class ModifyStrategy{
        SMALL_CHANGE,
        LARGE_JUMP,
        REDUCE_PRECISION
    }

}