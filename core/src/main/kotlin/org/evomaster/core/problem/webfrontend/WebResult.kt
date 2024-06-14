package org.evomaster.core.problem.webfrontend

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import java.net.URL

class WebResult : ActionResult {

    companion object{
        //Note: use WebPageIdentifier to retrieve shape from id
        /** Starting page id before the action */
        const val IDENTIFYING_PAGE_ID_START = "IDENTIFYING_PAGE_ID_START"
        /** Resulting page id after the action */
        const val IDENTIFYING_PAGE_ID_END   = "IDENTIFYING_PAGE_ID_END"
        /** Ids of the actions that were possible on the page before the executed action*/
        const val POSSIBLE_ACTION_IDS = "POSSIBLE_ACTION_IDS"
        const val URL_PAGE_START = "URL_PAGE_START"
        const val URL_PAGE_END = "URL_PAGE_END"
        const val VALID_HTML = "VALID_HTML"
    }

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    internal constructor(other: ActionResult) : super(other)

    override fun copy(): ActionResult {
        return WebResult(this)
    }


    fun setValidHtml(isValid: Boolean) = addResultValue(VALID_HTML, isValid.toString())

    fun getValidHtml() : Boolean? = getResultValue(VALID_HTML)?.toBoolean()

    fun setIdentifyingPageIdStart(shape: String) = addResultValue(IDENTIFYING_PAGE_ID_START, shape)

    fun getIdentifyingPageIdStart() : String? = getResultValue(IDENTIFYING_PAGE_ID_START)

    fun setUrlPageStart(url: String) = addResultValue(URL_PAGE_START, url)

    fun getUrlPageStart() : String? = getResultValue(URL_PAGE_START)

    fun setUrlPageEnd(url: String) = addResultValue(URL_PAGE_END, url)

    fun getUrlPageEnd() : String? = getResultValue(URL_PAGE_END)

    fun setIdentifyingPageIdEnd(shape: String) = addResultValue(IDENTIFYING_PAGE_ID_END, shape)

    fun getIdentifyingPageIdEnd() : String? = getResultValue(IDENTIFYING_PAGE_ID_END)

    fun setPossibleActionIds(ids: List<String>) = addResultValue(POSSIBLE_ACTION_IDS, ids.joinToString("\n"))

    fun getPossibleActionIds() : List<String> = getResultValue(POSSIBLE_ACTION_IDS)?.split("\n")?.toList()  ?: listOf()

    override fun matchedType(action: Action): Boolean {
        return action is WebAction
    }
}