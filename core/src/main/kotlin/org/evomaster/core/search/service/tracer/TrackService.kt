package org.evomaster.core.search.service.tracer

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TrackService{

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TrackService::class.java)
    }
    /**
     * id: type of TraceableElement
     * values: type of registered TrackOperator
     * This map indicates what operators are registered to handle TraceableElement
     */
    @Inject
    private lateinit var config: EMConfig

    fun init(){
        if(config.enableTrackIndividual)
            TraceableElement.maxlength = config.trackLength
    }


}