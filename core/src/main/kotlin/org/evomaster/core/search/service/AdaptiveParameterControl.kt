package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import kotlin.math.roundToInt

/**
 * Search algorithm parameters might change during the search,
 * eg based on time or fitness feedback
 */
class AdaptiveParameterControl {

    @Inject
    private lateinit var time : SearchTimeController

    @Inject
    private lateinit var config: EMConfig


    fun getArchiveTargetLimit() : Int {
        return getExploratoryValue(config.archiveTargetLimit, 1)
    }

    fun getProbRandomSampling(): Double {
        return getExploratoryValue(config.probOfRandomSampling , 0.0)
    }

    fun getNumberOfMutations(): Int {
        return getExploratoryValue(config.startNumberOfMutations, config.endNumberOfMutations )
    }

    fun getBaseTaintAnalysisProbability() : Double {
        return getExploratoryValue(config.baseTaintAnalysisProbability, 0.0)
    }

    /**
     * Based on the current state of the search, ie how long has been passed
     * and how much budget is left before starting a focused search,
     * return  a value between [start] (at the beginning of the search) and [end]
     * (when the focused search starts)
     */
    fun getExploratoryValue(start: Int, end: Int) : Int{
        return getExploratoryValue(start.toDouble(), end.toDouble()).roundToInt()
    }

    /**
     * Based on the current state of the search, ie how long has been passed
     * and how much budget is left before starting a focused search,
     * return  a value between [start] (at the beginning of the search) and [end]
     * (when the focused search starts)
     */
    fun getExploratoryValue(start: Double, end: Double) : Double{

        val passed: Double = time.percentageUsedBudget()
        val threshold:Double = config.focusedSearchActivationTime

        if(passed >= threshold){
            return end
        }

        val scale = passed / threshold

        val delta = end - start

        return start + (scale * delta)
    }

    /**
     * whether the search reaches the phase of 'focus search'
     */
    fun doesFocusSearch() : Boolean = time.percentageUsedBudget() >= config.focusedSearchActivationTime

}