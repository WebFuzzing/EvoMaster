package org.evomaster.core.utils

object CollectionUtils {


    /**
     * @return a map of elements as key, if they appear more than once in the input [list].
     * The number in the map is the number of occurrences of repetitions for such element.
     *
     * This means that lowest returned value in the map here is a 2
     */
    fun <T> duplicates(list: List<T>) : Map<T, Int> {

        return list.associateBy({ it },{ list.count { e -> it == e } })
            .filter { it.value > 1 }
    }
}