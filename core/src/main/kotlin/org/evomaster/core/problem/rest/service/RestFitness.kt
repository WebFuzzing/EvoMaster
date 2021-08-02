package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.StaticCounter
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class RestFitness : AbstractRestFitness<RestIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestFitness::class.java)
    }

    @Inject
    private lateinit var sampler: RestSampler

    override fun doCalculateCoverage(individual: RestIndividual, targets: Set<Int>): EvaluatedIndividual<RestIndividual>? {

        rc.resetSUT()

        val cookies = getCookies(individual)
        val tokens = getTokens(individual)

        if (log.isTraceEnabled){
            log.trace("do evaluate the individual, which contains {} dbactions and {} rest actions",
                individual.seeInitializingActions().size,
                individual.seeActions().size)
        }

        val actionResults: MutableList<ActionResult> = mutableListOf()

        doDbCalls(individual.seeInitializingActions(), actionResults = actionResults)


        val fv = FitnessValue(individual.size().toDouble())


        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        for (i in 0 until individual.seeActions().size) {

            val a = individual.seeActions()[i]

            if (log.isTraceEnabled){
                log.trace("handle rest action at index {}, and the action is {}, and the genes are",
                    i,
                    if (a is RestCallAction)  "${a.verb}:${a.resolvedPath()}" else a.getName(),
                    a.seeGenes().joinToString(","){
                        "${it::class.java.simpleName}:${
                            try {
                                it.getValueAsRawString()
                            }catch (e: Exception){
                                "null"
                            }
                        }"
                    }
                )
            }

            registerNewAction(a, i)

            var ok = false

            if (a is RestCallAction) {
                ok = handleRestCall(a, actionResults, chainState, cookies, tokens)
                /*
                    the action might be stopped due to e.g., timeout (see [handleRestCall]),
                    but the property of [stopping] is not handle.
                    we can also handle the property inside [handleRestCall]
                 */
                actionResults.filterIsInstance<RestCallResult>()[i].stopping = !ok
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }
        }

        if (log.isTraceEnabled){
            log.trace("evaluation ends")
        }

        val restActionResults = actionResults.filterIsInstance<RestCallResult>()
        restActionResultHandling(individual, targets, restActionResults, fv)?:return null

        if (log.isTraceEnabled){
            log.trace("restActionResult are handled")
        }

        if (actionResults.size > individual.seeActions(ActionFilter.ALL).size)
            log.warn("initialize invalid evaluated individual")

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)
    }




    override fun hasParameterChild(a: RestCallAction): Boolean {
        return sampler.seeAvailableActions()
                .filterIsInstance<RestCallAction>()
                .map { it.path }
                .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }
}