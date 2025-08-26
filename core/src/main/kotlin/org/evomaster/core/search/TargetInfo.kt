package org.evomaster.core.search

/**
 * Information about a "testing-target" achieved (or not) during the execution of a test case.
 * Targets could be executed statements/branches in the source code, achieved HTTP status responses, etc.
 */
class TargetInfo (
    /**
     * Unique identifier for this target.
     * The actual value does not matter, as long as it is unique and not changing throughout the search (ie, no time-stamps).
     * Descriptive ids are useful also during debugging.
     */
    val descriptiveId: String,
    /**
     * Heuristic value in range [0,1].
     * The value 1 means the target has been covered.
     * Otherwise, not covered, where values closer to 1 represent heuristically how close it was to cover them.
     */
    val value: Double,
    /**
     *  A test case could be composed of several actions (eg HTTP calls).
     *  Here, _optionally_ we can keep track of in which action the target was covered.
     *  If this information is missing or not collected, or the heuristic [value] is 0,
     *  then this index must keep a negative value.
     */
    val actionIndex: Int = -1
){

    init {
        if(value !in 0.0..1.0){
            throw IllegalArgumentException("Heuristic value must be in range [0..1]")
        }
    }
}