package org.evomaster.core.search


data class Heuristics(
        /** heuristic distance in [0,1], where 1 is for "covered" */
        val distance: Double,
        /** The index of the action that lead to the heuristics value.
         *  A negative value means the info is not available */
        val actionIndex: Int)