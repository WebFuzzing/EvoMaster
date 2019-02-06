package org.evomaster.core.search.service.tracer

interface TrackOperator {

    companion object {
        const val method_separator = "::"
    }

    fun getTrackOperator() : String = this :: class.java.simpleName

    fun appendMethod(value : String) : String = value + method_separator + getTrackOperator()


}