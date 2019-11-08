package org.evomaster.core.search.tracer

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import javax.annotation.PostConstruct

/**
 * handle how to copy a [TraceableElement] based on [EMConfig]
 */
class ArchiveMutationTrackService : TrackService(){

    @Inject
    private lateinit var config: EMConfig

    @PostConstruct
    private fun postConstruct(){

        if (config.enableTrackIndividual || config.enableTrackEvaluatedIndividual) {
            initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_INDIVIDUAL)
        }

        if (config.enableTrackEvaluatedIndividual && config.probOfArchiveMutation > 0.0){
            initTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT)
            initTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_COPY_IMPACT)
        }
    }

    private fun initTraceableElementCopyFilter(name : String){
        register(object : TraceableElementCopyFilter(name){
            override fun accept(element: Any): Boolean = element is EvaluatedIndividual<*>
        })
    }


    fun getCopyFilterForEvalInd(chosen: EvaluatedIndividual<*>, deepCopyForImpacts : Boolean = false) : TraceableElementCopyFilter {
        return if (config.enableTrackEvaluatedIndividual && config.probOfArchiveMutation > 0.0){
            if (deepCopyForImpacts){
                if (!exists(EvaluatedIndividual.WITH_TRACK_WITH_COPY_IMPACT)){
                    throw IllegalStateException("WITH_TRACK_WITH_CLONE_IMPACT should be registered.")
                }
                getTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT, chosen)
            }
            if (!exists(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT)){
                throw IllegalStateException("WITH_TRACK_WITH_CLONE_IMPACT should be registered.")
            }
            getTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT, chosen)
        }else if (config.enableTrackEvaluatedIndividual)
            TraceableElementCopyFilter.WITH_TRACK
        else if (config.enableTrackIndividual) {
            if (!exists(EvaluatedIndividual.ONLY_INDIVIDUAL)){
                throw IllegalStateException("ONLY_INDIVIDUAL should be registered.")
            }
            getTraceableElementCopyFilter(EvaluatedIndividual.ONLY_INDIVIDUAL, chosen)
        }
        else TraceableElementCopyFilter.NONE
    }

}