package org.evomaster.core.search.gene

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.utils.NumberCalculationUtil.calculateIncrement

abstract class FloatingPointNumber<T:Number>(
    name: String,
    value: T,
    min: T? = null,
    max: T? = null,
    /**
     * specified precision
     */
    val precision: Int?,
    /**
     * specified scale
     */
    val scale: Int?
) : NumberGene<T>(name, value, min, max){

    enum class ModifyStrategy{
        //for small changes
        SMALL_CHANGE,
        //for large jumps
        LARGE_JUMP,
        //to reduce precision, ie chop off digits after the "."
        REDUCE_PRECISION
    }


    private fun getMaxRange(direction: Double): Long {
        return if (!isRangeSpecified()) Long.MAX_VALUE
        else if (direction > 0)
            calculateIncrement(value.toDouble(), getMaximum().toDouble()).toLong()
        else
            calculateIncrement(getMinimum().toDouble(), value.toDouble()).toLong()
    }

    /**
     * mutate Floating Point Number in a standard way
     */
    fun mutateFloatingPointNumber(randomness: Randomness, apc: AdaptiveParameterControl): T{
        return NumberMutator.mutateFloatingPointNumber(randomness, null, maxRange = null, apc, value, smin = getMinimum(), smax = getMaximum(), scale=scale)
    }

    /**
     * @return formatted [value] based on [scale]
     */
    fun getFormattedValue(valueToFormat: T?=null) : T{
        return NumberMutator.getFormattedValue(valueToFormat?:value, scale)
    }

    /**
     * @return minimal changes of the [value].
     * this is typlically used when [scale] is specified
     */
    fun getMinimalDelta(): T?{
        return NumberMutator.getMinimalDelta(scale, value)
    }

    /**
     * @return whether the gene is valid that considers
     *      1) within min..max if they are specified
     *      2) precision if it is specified
     */
    override fun isValid(): Boolean {
        return super.isValid() && (scale == null || !value.toString().contains(".") || value.toString().split(".")[1].length <= scale)
    }
}