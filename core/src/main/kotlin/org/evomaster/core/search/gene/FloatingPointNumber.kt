package org.evomaster.core.search.gene

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.utils.NumberCalculationUtil.valueWithPrecision
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min
import kotlin.math.pow

abstract class FloatingPointNumber<T:Number>(
    name: String,
    value: T,
    min: T? = null,
    max: T? = null,
    /**
     * specified precision
     */
    val precision: Int?
) : NumberGene<T>(name, value, min, max){


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


    enum class ModifyStrategy{
        //for small changes
        SMALL_CHANGE,
        //for large jumps
        LARGE_JUMP,
        //to reduce precision, ie chop off digits after the "."
        REDUCE_PRECISION
    }


    /**
     * @return formatted value based on precision if it has
     */
    fun getFormattedValue(valueToFormat: T? = null) : T {
        val fvalue = valueToFormat?:value
        if (precision == null)
            return fvalue
        return when (fvalue) {
            is Double -> valueWithPrecision(fvalue.toDouble(), precision).toDouble() as T
            is Float -> valueWithPrecision(fvalue.toDouble(), precision).toFloat() as T
            else -> throw Exception("valueToFormat must be Double or Float, but it is ${fvalue::class.java.simpleName}")
        }
    }

    /**
     * @return minimal delta if it has.
     * this is typically used when the precision is specified
     */
    fun getMinimalDelta(): T? {
        if (precision == null) return null
        val mdelta = 1.0/((10.0).pow(precision))
        return when (value) {
            is Double -> valueWithPrecision(mdelta, precision).toDouble() as T
            is Float -> valueWithPrecision(mdelta, precision).toFloat() as T
            else -> throw Exception("valueToFormat must be Double or Float, but it is ${value::class.java.simpleName}")
        }
    }

    /**
     * mutate double/float number
     */
    fun mutateFloatingPointNumber(randomness: Randomness, apc: AdaptiveParameterControl): T{

        var gaussianDelta = randomness.nextGaussian()
        if (gaussianDelta == 0.0)
            gaussianDelta = randomness.nextGaussian()

        if ((max != null && max == value && gaussianDelta > 0) || (min != null && min == value && gaussianDelta < 0) )
            gaussianDelta *= -1.0

        val maxRange = getMaxRange(gaussianDelta)

        var res = modifyValue(randomness, value.toDouble(), delta = gaussianDelta, maxRange = maxRange, specifiedJumpDelta = GeneUtils.getDelta(randomness, apc, maxRange),precision == null)

        if (precision != null && getFormattedValue() == getFormattedValue(res as T)){
            res += (if (gaussianDelta>0) 1.0 else -1.0).times(getMinimalDelta()!!.toDouble())
        }

        return if (max != null && res > max!!.toDouble()) max!!
                else if (min != null && res < min!!.toDouble()) min!!
                else getFormattedValue(res as T)
    }

    abstract fun getMaxRange(direction: Double) : Long
}