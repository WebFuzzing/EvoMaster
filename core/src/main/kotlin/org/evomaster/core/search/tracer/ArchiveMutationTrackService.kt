package org.evomaster.core.search.tracer

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import javax.annotation.PostConstruct

/**
 * handle how to copy a [Traceable] based on [EMConfig]
 */
class ArchiveMutationTrackService : TrackService(){

    @Inject
    private lateinit var config: EMConfig

    @PostConstruct
    fun postConstruct(){

        if (config.trackingEnabled()) {
            initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED)
        }

        if (config.enableTrackEvaluatedIndividual && config.isEnabledImpactCollection()){
            initTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT)
            initTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_COPY_IMPACT)
        }

        if (config.isEnabledImpactCollection()){
            initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_WITH_COPY_IMPACT)
            initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_WITH_CLONE_IMPACT)
        }
    }

    private fun initTraceableElementCopyFilter(name : String){
        register(object : TraceableElementCopyFilter(name){
            override fun accept(element: Any): Boolean = element is EvaluatedIndividual<*>
        })
    }


    fun getCopyFilterForEvalInd(chosen: EvaluatedIndividual<*>) : TraceableElementCopyFilter {
        return if (config.enableTrackEvaluatedIndividual && config.isEnabledImpactCollection()){
            if (!exists(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT)){
                throw IllegalStateException("WITH_TRACK_WITH_CLONE_IMPACT should be registered.")
            }
            getTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT, chosen)
        }else if(config.isEnabledImpactCollection()){
            if (!exists(EvaluatedIndividual.ONLY_WITH_CLONE_IMPACT)){
                throw IllegalStateException("ONLY_WITH_CLONE_IMPACT should be registered.")
            }
            getTraceableElementCopyFilter(EvaluatedIndividual.ONLY_WITH_CLONE_IMPACT, chosen)
        }else if (config.enableTrackEvaluatedIndividual)
            TraceableElementCopyFilter.WITH_TRACK
        else if (config.enableTrackIndividual) {
            if (!exists(EvaluatedIndividual.ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED)){
                throw IllegalStateException("ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED should be registered.")
            }
            getTraceableElementCopyFilter(EvaluatedIndividual.ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED, chosen)
        }
        else TraceableElementCopyFilter.NONE
    }

}