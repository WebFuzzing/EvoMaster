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

    fun updateCounter(improved : Int){
        if (improved > 0) counter = 0
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
    abstract fun random(apc: AdaptiveParameterControl, randomness: Randomness, current: T, probOfMiddle : Double, start: Int, end: Int, minimalTimeForUpdate: Int) : T

    abstract fun doReset(current : T, evaluatedResult: Int) : Boolean
    abstract fun updateBoundary(current: T, evaluatedResult : Int)

    fun updateOrRestBoundary(current: T, evaluatedResult : Int){
        if (doReset(current, evaluatedResult)){
            reset()
        }else{
            updateBoundary(current, evaluatedResult)
        }
        updateCounter(evaluatedResult)
        latest = current
    }

    fun updateOrRestBoundary(history : List<Pair<T, Int>>){
        (0 until history.size).forEach {i->
            updateOrRestBoundary(
                    current = history[i].first,
                    evaluatedResult = history[i].second
            )
        }
    }
}





