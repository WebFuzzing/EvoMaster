package org.evomaster.core.search.service.mutator.genemutation.mutationupdate

import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LongMutationUpdate(min: Long, max: Long, updateTimes : Int = 0, counter: Int = 0, reached: Boolean = false, latest : Long? = null, preferMin : Long= min, preferMax: Long = max)
    : MutationBoundaryUpdate<Long>(min, max, counter = counter, updateTimes = updateTimes, reached = reached, latest = latest, preferMin = preferMin, preferMax = preferMax), Comparable<LongMutationUpdate>{

    constructor(min: Int, max: Int) : this(min = min.toLong(), max = max.toLong())

    override fun doReset(current: Long, doesCurrentBetter: Boolean): Boolean {
        return (current < preferMin || current > preferMax) && doesCurrentBetter
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(LongMutationUpdate::class.java)
    }

    override fun middle(): Long {
        if (preferMin > preferMax)
            log.warn("min {} should not be more than max {}", preferMin, preferMax)
        return (preferMin/2.0 + preferMax/2.0).toLong()
    }

    override fun random(apc: AdaptiveParameterControl, randomness: Randomness, current: Long, probOfMiddle: Double, start: Int, end: Int, minimalTimeForUpdate: Int): Long {
        var c = current
        if (randomness.nextBoolean(probOfMiddle)) {
            val middle = middle()
            if (middle != current) return middle
            c = middle
        }
        val delta = GeneUtils.getDelta(randomness, apc, range = candidatesBoundary(), start = start, end = end)
        val candidates = listOf(c + delta, c - delta)
        val valid = candidates.filter { it <= preferMax && it >= preferMin }
        return when {
            valid.isNotEmpty() -> randomness.choose(valid)
            candidates.min()!! > preferMax -> preferMax
            else -> preferMin
        }
    }

    override fun compareTo(other: LongMutationUpdate): Int {
        val result = try{
            Math.subtractExact(other.candidatesBoundary(), candidatesBoundary())
        }catch (e : ArithmeticException) {
            return other.candidatesBoundary().compareTo(candidatesBoundary())
        }
        return if (result > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else if (result < Int.MIN_VALUE.toLong()) Int.MIN_VALUE else result.toInt()
    }

    override fun candidatesBoundary(): Long {
        val range = try {
            Math.subtractExact(preferMax, preferMin)
        }catch (e : ArithmeticException){
            return  Long.MAX_VALUE
        }
        return range.also {
            if (it < 0) throw IllegalStateException("preferMax < preferMin: $preferMax, $preferMin")
        }
    }

    override fun copy(): LongMutationUpdate = LongMutationUpdate(preferMin, preferMax, updateTimes, counter, reached, latest, preferMin, preferMax)

    override fun updateBoundary(current: Long, doesCurrentBetter: Boolean) {
        latest?:return
        if (current == latest) return
        val value = latest!!/2.0 + current/2.0
        val updateMin = (doesCurrentBetter && current > latest!!) || (!doesCurrentBetter && current < latest!!)
        if (updateMin){
            preferMin = if (value > preferMax) {
                if (value.toLong()+1L > preferMax) preferMax
                else value.toLong()+1L
            } else value.toLong()
        }else{
            preferMax = if(value < preferMin) {
                if(value.toLong() +1L < preferMin) preferMin
                else value.toLong() +1L
            } else value.toLong()
        }
        updateTimes +=1
    }
}