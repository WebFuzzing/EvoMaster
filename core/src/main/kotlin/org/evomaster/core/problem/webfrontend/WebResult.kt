package org.evomaster.core.problem.webfrontend

import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult

class WebResult : ActionResult {

    companion object{
        //Note: use WebPageIdentifier to retrieve shape from id
        /** Starting page id before the action */
        const val IDENTIFYING_PAGE_ID_START = "IDENTIFYING_PAGE_ID_START"
        /** Resulting page id after the action */
        const val IDENTIFYING_PAGE_ID_END   = "IDENTIFYING_PAGE_ID_END"
        /** Ids of the actions that were possible on the page before the executed action*/
        const val POSSIBLE_ACTION_IDS = "POSSIBLE_ACTION_IDS"
    }

    constructor(stopping: Boolean = false) : super(stopping)

    internal constructor(other: ActionResult) : super(other)

    override fun copy(): ActionResult {
        return WebResult(this)
    }

    /*
        TODO: possible optimization. as pages are supposed to be finite, we could save them in a central map,
        and here in results just save a pointer. potentially useful for saving memory
     */

    fun setIdentifyingPageIdStart(shape: String) = addResultValue(IDENTIFYING_PAGE_ID_START, shape)

    fun getIdentifyingPageIdStart() : String? = getResultValue(IDENTIFYING_PAGE_ID_START)

    fun setIdentifyingPageIdEnd(shape: String) = addResultValue(IDENTIFYING_PAGE_ID_END, shape)

    fun getIdentifyingPageIdEnd() : String? = getResultValue(IDENTIFYING_PAGE_ID_END)

    fun setPossibleActionIds(ids: List<String>) = addResultValue(POSSIBLE_ACTION_IDS, ids.joinToString { "\n" })

    fun getPossibleActionIds() : List<String> = getResultValue(POSSIBLE_ACTION_IDS)?.split("\n")?.toList()  ?: listOf()

    override fun matchedType(action: Action): Boolean {
        return action is WebAction
    }
}