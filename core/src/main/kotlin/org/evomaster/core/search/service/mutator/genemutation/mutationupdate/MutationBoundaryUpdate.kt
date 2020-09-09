package org.evomaster.core.search.service.mutator.genemutation.mutationupdate

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness

/**
 * created by manzh on 2019-09-19
 */
abstract class MutationBoundaryUpdate<T> (
        val min : T, val max : T, var counter : Int = 0, var reached : Boolean = false,
        var updateTimes : Int = 0,
        var resetTimes : Int = 0, var latest: T? = null, var preferMin : T = min, var preferMax : T = max) where T : Number{



    abstract fun copy() : MutationBoundaryUpdate<T>
    abstract fun candidatesBoundary() : T

    fun updateCounter(improved : Boolean){
        if (improved) counter = 0
        else counter += 1
    }

    fun reset(){
        preferMin = min
        preferMax = max
        resetTimes += 1
        reached = false
    }

    fun isReached(current : T) : Boolean = preferMin == preferMax && preferMin == current

    abstract fun middle() : T
    abstract fun random(apc: AdaptiveParameterControl, randomness: Randomness, current: T, probOfMiddle : Double, start: Int, end: Int) : T

    abstract fun doReset(current : T, doesCurrentBetter: Boolean) : Boolean
    abstract fun updateBoundary(current: T, doesCurrentBetter : Boolean)

    fun updateOrRestBoundary(current: T, doesCurrentBetter : Boolean){
        if (doReset(current, doesCurrentBetter)){
            reset()
        }else{
            updateBoundary(current, doesCurrentBetter)
        }
        updateCounter(doesCurrentBetter)
        latest = current
    }
}





