package org.evomaster.core.search.tracer

import org.evomaster.core.problem.rest.util.ParamUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * created by manzh on 2019-09-06
 *
 * this is used to decide applicable copy filter for [TraceableElement]
 */
abstract class TraceableElementCopyFilter (val name : String) {

    companion object{

        /**
         * without tracking
         */
        val NONE = object : TraceableElementCopyFilter("NONE"){
        }

        /**
         * with tracking
         */
        val WITH_TRACK = object :  TraceableElementCopyFilter("WITH_TRACK"){
        }

        /**
         * with deep tracking, i.e., tracking nested TraceableElement
         */
        val DEEP_TRACK = object : TraceableElementCopyFilter("DEEP_TRACK"){
        }
    }

    /**
     * check if accept the [element] is applicable with the copy filter
     */
    open fun accept(element : Any): Boolean = element is TraceableElement

}