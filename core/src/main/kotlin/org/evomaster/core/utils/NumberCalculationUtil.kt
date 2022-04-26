package org.evomaster.core.utils

import org.evomaster.core.search.gene.NumberMutatorUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min
import kotlin.math.pow

object NumberCalculationUtil {

    val log: Logger = LoggerFactory.getLogger(NumberCalculationUtil::class.java)

    /**
     * calculate the maximum increment for double which should be [min, max],
     * the maximum delta should be less than [maxRange]
     */
    fun calculateIncrement(min: Double, max: Double, maxRange: Double = Long.MAX_VALUE.toDouble()) : Double{
        if (!validNumber(min) || !validNumber(max))
            throw IllegalStateException("invalid number")
        val result = BigDecimal(max).subtract(BigDecimal(min)).toDouble()
        if (result.isInfinite() || result.isNaN() || result > maxRange) return maxRange
        return result
    }

    private fun validNumber(value: Double) : Boolean{
        return !(value.isInfinite() || value.isNaN())
    }


    /**
     * calculate the maximum increment for long which should be [min, max]
     */
    fun calculateIncrement(min: Long, max: Long, minIncrement: Long =1L, maxIncrement: Long= Long.MAX_VALUE) : Long{
        return try{
            min(maxIncrement, Math.addExact(Math.subtractExact(max, min), minIncrement))
        }catch (e : ArithmeticException) {
            maxIncrement
        }
    }

    /**
     * get boundary of the decimal which has specified precision and scale
     * eg, for Decimal (3,2), the boundary is [-99.9, 99.9]
     */
    fun boundaryDecimal(size: Int, scale: Int, roundingMode: RoundingMode= RoundingMode.HALF_UP): Pair<BigDecimal, BigDecimal>{
        if (size > NumberMutatorUtils.MAX_DOUBLE_EXCLUSIVE){
            log.warn("there would exist error if the precision is greater than 15")
        }

        val integral = (10.0).pow(size) - 1
        val fraction = (10.0).pow(scale)
        val boundary = integral.div(fraction)
        return valueWithPrecisionAndScale(boundary * -1, scale) to valueWithPrecisionAndScale(boundary * 1, scale, roundingMode)
    }

    /**
     * @return decimal for double with the specified scale
     */
    fun valueWithPrecisionAndScale(value: Double, scale: Int?, roundingMode: RoundingMode = RoundingMode.HALF_UP) : BigDecimal {
        return try {
            if (scale == null)
                BigDecimal.valueOf(value)
            else
                BigDecimal(value).setScale(scale, roundingMode)
        }catch (e: NumberFormatException){
            log.warn("fail to get value ($value) with the specified prevision ($scale)")
            throw e
        }
    }

    /**
     * @return get middle value of the two values
     */
    fun <T: Number> getMiddle(min: T, max : T, scale: Int?) : BigDecimal{
        val m = min.toDouble()/2.0  + max.toDouble()/2.0
        return valueWithPrecisionAndScale(m, scale)
    }
}