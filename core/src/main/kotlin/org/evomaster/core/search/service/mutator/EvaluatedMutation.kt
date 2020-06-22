package org.evomaster.core.search.service.mutator

/**
 * comparing mutated with current, eg,
 *  [BETTER_THAN] means that the mutated is better than the current.
 */
enum class EvaluatedMutation(val value : Int){
    BETTER_THAN(1),
    EQUAL_WITH(0),
    WORSE_THAN(-1);

    fun isEffective() = value >=0

}