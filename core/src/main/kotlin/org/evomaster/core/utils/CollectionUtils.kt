package org.evomaster.core.utils

import java.util.function.BiFunction

object CollectionUtils {


    /**
     * @return a map of elements as key, if they appear more than once in the input [list].
     * The number in the map is the number of occurrences of repetitions for such element.
     *
     * This means that lowest returned value in the map here is a 2
     */
    fun <T> duplicates(list: List<T>) : Map<T, Int> {

        return list.groupingBy { it }.eachCount()
            //previous implementation was too inefficient
//             return   list.associateBy({ it },{ list.count { e -> it == e } })
            .filter { it.value > 1 }
    }

    /**
     * Return a sublist on the input [list], where only "distinct" elements are returned.
     * This is based on the provided [equivalentLambda], which should return whether 2 elements
     * are equivalent.
     */
    fun <T> deDuplicate(list: List<T>, equivalentLambda: BiFunction<T,T, Boolean>) : List<T>{

        val result = mutableListOf<T>()
        for(x in list){
            if(result.any { equivalentLambda.apply(it, x) }){
                continue
            }
            result.add(x)
        }

        return result
    }
}
