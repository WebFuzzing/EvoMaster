package org.evomaster.core.search.tracer

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.impactInfoCollection.GeneMutationSelectionMethod
import javax.annotation.PostConstruct

/**
 * handle how to copy a [TraceableElement] based on [EMConfig]
 */
class ArchiveMutationTrackService : TrackService(){

    @Inject
    private lateinit var config: EMConfig

    @PostConstruct
    fun postConstruct(){

        if (config.trackingEnabled()) {
            initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED)
        }

        if (config.enableTrackEvaluatedIndividual && config.collectImpact()){
            initTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT)
            initTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_COPY_IMPACT)
            initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_WITH_COPY_IMPACT)
            initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_WITH_CLONE_IMPACT)
        }
    }

    private fun initTraceableElementCopyFilter(name : String){
        register(object : TraceableElementCopyFilter(name){
            override fun accept(element: Any): Boolean = element is EvaluatedIndividual<*>
        })
    }


    fun getCopyFilterForEvalInd(chosen: EvaluatedIndividual<*>, deepCopyForImpacts : Boolean = false) : TraceableElementCopyFilter {
        return if (config.enableTrackEvaluatedIndividual && ((config.probOfArchiveMutation > 0.0 && config.adaptiveGeneSelectionMethod != GeneMutationSelectionMethod.NONE) || config.doCollectImpact)){
            if (deepCopyForImpacts){
                if (!exists(EvaluatedIndividual.WITH_TRACK_WITH_COPY_IMPACT)){
                    throw IllegalStateException("WITH_TRACK_WITH_CLONE_IMPACT should be registered.")
                }
                getTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT, chosen)
            }else{
                if (!exists(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT)){
                    throw IllegalStateException("WITH_TRACK_WITH_CLONE_IMPACT should be registered.")
                }
                getTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT, chosen)
            }
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