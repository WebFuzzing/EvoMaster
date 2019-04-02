package org.evomaster.core.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker
import org.slf4j.MarkerFactory


/**
 * Global log filter used to do not repeat logs (ie make them unique)
 * having a specific marker
 *
 * Created by arcuri82 on 02-Apr-19.
 */
class UniqueTurboFilter : TurboFilter() {

    companion object {
        val UNIQUE_MARKER = MarkerFactory.getMarker("unique_marker_for_logs")!!

        private val uniqueMessages = mutableSetOf<String>()
    }

    override fun decide(marker: Marker?,
                        logger: Logger?,
                        level: Level?,
                        format: String?,
                        params: Array<out Any>?,
                        t: Throwable?): FilterReply {

        if(marker != UNIQUE_MARKER){
            return FilterReply.NEUTRAL
        }

        val msgKey = format + "_" + params?.map { it.toString() }?.joinToString("_")

        if(uniqueMessages.contains(msgKey)){
            return FilterReply.DENY
        }

        uniqueMessages.add(msgKey)

        return FilterReply.NEUTRAL
    }
}