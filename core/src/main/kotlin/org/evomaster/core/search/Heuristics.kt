package org.evomaster.core.search


data class Heuristics(
        /**
         * heuristic score in [0,1], where 1 is for "covered"/"achieved" target.
         * Note: this is different from branch "distance" (d), in concept, as there covered is 0.
         * But two values are equivalent: d = 1 - h
         * Inside EvoMaster we use "heuristic score" per target instead of "distance" because we do not
         * know the number of total targets (it is dynamic).
         * Adding heuristic scores together makes senses, whereas it would be not so useful for distances.
         */
        val score: Double,
        /** The index of the action that lead to the heuristics value.
         *  A negative value means the info is not available */
        val actionIndex: Int)