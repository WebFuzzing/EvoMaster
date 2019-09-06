package org.evomaster.core.search.tracer

import org.evomaster.core.problem.rest.util.ParamUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * created by manzh on 2019-09-06
 */
abstract class TraceableElementCopyFilter (val name : String) {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(TraceableElementCopyFilter::class.java)

        val filter = mutableMapOf<String, TraceableElementCopyFilter>()

        val NONE = object : TraceableElementCopyFilter("NONE"){
            init {
                register(this)
            }
        }

        val WITH_TRACK = object :  TraceableElementCopyFilter("WITH_TRACK"){
            init {
                register(this)
            }
        }
        val DEEP_TRACK = object : TraceableElementCopyFilter("DEEP_TRACK"){
            init {
                register(this)
            }
        }

        fun getTraceableElementCopyFilter(name: String, element: Any) : TraceableElementCopyFilter{
            val filter = filter[name]?:throw IllegalArgumentException("the filter with $name does not exist.")
            if (filter.accept(element)) return  filter
            throw IllegalArgumentException("the element cannot be applied with the filter with $name")
        }

        fun register(copyFilter: TraceableElementCopyFilter){
            if (filter.containsKey(copyFilter.name)){
                log.warn("a filter with ${copyFilter.name} has been registered!")
            }
            filter[copyFilter.name] = copyFilter
        }

        fun exists(name: String) = filter.containsKey(name)
    }

    open fun accept(element : Any): Boolean = element is TraceableElement

}