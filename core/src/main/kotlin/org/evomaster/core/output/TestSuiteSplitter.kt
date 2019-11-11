package org.evomaster.core.output

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Solution


/**
 * Created by arcuri82 on 11-Nov-19.
 */
object TestSuiteSplitter {


    /**
     * Given a [Solution], split it into several smaller solutions, based on the given [type] strategy.
     * No test must be lost, and combining/aggregating all those smaller solutions should give back
     * the original [Solution]
     */
    fun split(solution: Solution<*>, type: EMConfig.TestSuiteSplitType) : List<Solution<*>>{

        return when(type){
            EMConfig.TestSuiteSplitType.NONE -> listOf(solution)
        }
    }
}