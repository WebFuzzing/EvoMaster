package org.evomaster.core.search.tracer

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * created by manzh on 2019-09-12
 */
abstract class TrackService {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(TrackService::class.java)
    }
    /**
     * registered copy filter with name
     * key is name
     * value is copyfilter
     */
    private val filter = mutableMapOf<String, TraceableElementCopyFilter>()


    init{
        register(TraceableElementCopyFilter.NONE)
        register(TraceableElementCopyFilter.WITH_TRACK)
        register(TraceableElementCopyFilter.DEEP_TRACK)
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