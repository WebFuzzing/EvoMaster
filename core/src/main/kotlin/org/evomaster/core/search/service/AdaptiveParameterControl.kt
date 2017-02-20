package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.service.SearchTimeController

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
        return Math.round(getExploratoryValue(config.archiveTargetLimit, 1)).toInt()
    }

    fun getProbRandomSampling(): Double {
        return getExploratoryValue(config.probOfRandomSampling , 0.0)
    }

    fun getNumberOfMutations(): Int {
        return Math.round(getExploratoryValue(config.startNumberOfMutations, config.endNumberOfMutations )).toInt()
    }

    fun getExploratoryValue(start: Int, end: Int) : Double{
        return getExploratoryValue(start.toDouble(), end.toDouble())
    }

    /**
     * Based on the current state of the search, ie how long has been passed
     * and how much budget is left before starting a focused search,
     * return  a value between "start" (at the beginning of the search) and "end"
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

}