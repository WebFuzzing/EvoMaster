package org.evomaster.core.utils

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


    fun boundaryDecimal(size: Int, precision: Int): Pair<Double, Double>{
        val value = valueWithPrecision(10.0.pow(size-precision), precision)
        val p = valueWithPrecision(1.0/(10.0.pow(precision)), precision)
        val boundary = value.subtract(p).toDouble()
        return valueWithPrecision(boundary * -1, precision).toDouble() to valueWithPrecision(boundary * 1, precision).toDouble()
    }

    fun valueWithPrecision(value: Double, precision: Int) : BigDecimal {
        return try {
            BigDecimal(value).setScale(precision, RoundingMode.HALF_UP)
        }catch (e: NumberFormatException){
            log.warn("fail to get value ($value) with the specified prevision ($precision)")
            throw e
        }
    }
}