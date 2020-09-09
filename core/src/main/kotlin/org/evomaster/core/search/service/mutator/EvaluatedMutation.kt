package org.evomaster.core.search.service.mutator

/**
 * comparing mutated with current, eg,
 *  [BETTER_THAN] means that the mutated is better than the current.
 */
enum class EvaluatedMutation(val value : Int) {
    NEWLY_IDENTIFIED(2) ,
    BETTER_THAN(1),
    EQUAL_WITH(0),
    WORSE_THAN(-1),
    UNSURE(-2);

    fun isEffective() = value >=0

    fun isImpactful() = value !=0 && value != -2

    fun isImproved() = value > 0

    fun isWorse() = value == -1

    companion object{

        fun range() = -2..2

        fun range(min : Int = -2, max : Int = 2) = min..max

    }
}