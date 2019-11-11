package org.evomaster.core.output

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Solution


/**
 * Created by arcuri82 on 11-Nov-19.
 */
object TestSuiteSplitter {



    fun split(solution: Solution<*>, type: EMConfig.TestSuiteSplitType) : List<Solution<*>>{

        return when(type){
            EMConfig.TestSuiteSplitType.NONE -> listOf(solution)
        }
    }
}