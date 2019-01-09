package org.evomaster.core.search.service.tracer

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Archive Tracking -> Archive, EvaluatedIndividual
 * Individual
 */
class TrackingService{

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TrackingService::class.java)
    }

    private val operatorCounter : MutableMap<String, Int> = mutableMapOf()

    @Inject
    private lateinit var config: EMConfig

    fun init(){
        if(config.enableTrackIndividual)
            TraceableElement.maxlength = config.trackLength
    }


}