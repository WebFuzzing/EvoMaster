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
        private val log: Logger = LoggerFactory.getLogger(TraceableElementCopyFilter::class.java)

        /**
         * registered copy filter with name
         * key is name
         * value is copyfilter
         */
        private val filter = mutableMapOf<String, TraceableElementCopyFilter>()

        /**
         * without tracking
         */
        val NONE = object : TraceableElementCopyFilter("NONE"){
            init {
                register(this)
            }
        }

        /**
         * with tracking
         */
        val WITH_TRACK = object :  TraceableElementCopyFilter("WITH_TRACK"){
            init {
                register(this)
            }
        }

        /**
         * with deep tracking, i.e., tracking nested TraceableElement
         */
        val DEEP_TRACK = object : TraceableElementCopyFilter("DEEP_TRACK"){
            init {
                register(this)
            }
        }

        /**
         * get an applicable copy filter by [name] and [element]
         */
        fun getTraceableElementCopyFilter(name: String, element: Any) : TraceableElementCopyFilter{
            val filter = filter[name]?:throw IllegalArgumentException("the filter with $name does not exist.")
            if (filter.accept(element)) return  filter
            throw IllegalArgumentException("the element cannot be applied with the filter with $name")
        }

        /**
         * register new filter
         */
        fun register(copyFilter: TraceableElementCopyFilter){
            if (filter.containsKey(copyFilter.name)){
                log.warn("a filter with ${copyFilter.name} has been registered!")
            }
            filter[copyFilter.name] = copyFilter
        }

        /**
         * check if the copyfilter with [name] exists
         */
        fun exists(name: String) = filter.containsKey(name)
    }

    /**
     * check if accept the [element] is applicable with the copy filter
     */
    open fun accept(element : Any): Boolean = element is TraceableElement

}