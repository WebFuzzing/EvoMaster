package org.evomaster.core.search.tracer

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * created by manzh on 2019-09-06
 */
class ArchiveMutationTrackService {

    @Inject
    private lateinit var config: EMConfig

    companion object{
        private val log: Logger = LoggerFactory.getLogger(TraceableElementCopyFilter::class.java)
    }

    @PostConstruct
    private fun postConstruct(){
        if (config.enableTrackIndividual || config.enableTrackEvaluatedIndividual) {
            initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_INDIVIDUAL)
        }

        if (config.enableTrackEvaluatedIndividual && config.probOfArchiveMutation > 0.0){
            initTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_IMPACT)
        }
    }

    private fun initTraceableElementCopyFilter(name : String){
        TraceableElementCopyFilter.register(object : TraceableElementCopyFilter(name){
            override fun accept(element: Any): Boolean = element is EvaluatedIndividual<*>
        })
    }


    fun getCopyFilterForEvalInd(chosen: EvaluatedIndividual<*>) : TraceableElementCopyFilter {
        return if (config.enableTrackEvaluatedIndividual && config.probOfArchiveMutation > 0.0){
            if (!TraceableElementCopyFilter.exists(EvaluatedIndividual.WITH_TRACK_WITH_IMPACT)){
                log.warn("it should only happen when running tests, i.e., EMConfig might be modified after the service is initialed.")
                initTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_IMPACT)
            }
            TraceableElementCopyFilter.getTraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_IMPACT, chosen)
        }else if (config.enableTrackEvaluatedIndividual)
            TraceableElementCopyFilter.WITH_TRACK
        else if (config.enableTrackIndividual) {
            if (!TraceableElementCopyFilter.exists(EvaluatedIndividual.ONLY_INDIVIDUAL)){
                log.warn("it should only happen when running tests, i.e., EMConfig might be modified after the service is initialed.")
                initTraceableElementCopyFilter(EvaluatedIndividual.ONLY_INDIVIDUAL)
            }
            TraceableElementCopyFilter.getTraceableElementCopyFilter(EvaluatedIndividual.ONLY_INDIVIDUAL, chosen)
        }
        else TraceableElementCopyFilter.NONE
    }

}