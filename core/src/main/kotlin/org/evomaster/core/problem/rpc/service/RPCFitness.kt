package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.core.Lazy
import org.evomaster.core.problem.httpws.service.HttpWsFitness
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * created by manzhang on 2021/11/26
 */
class RPCFitness : HttpWsFitness<RPCIndividual>() {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(RPCFitness::class.java)
    }

    @Inject lateinit var convertor: RPCDtoConvertor

    override fun doCalculateCoverage(individual: RPCIndividual, targets: Set<Int>): EvaluatedIndividual<RPCIndividual>? {

        rc.resetSUT()

        // TOD handle auth
        val actionResults: MutableList<ActionResult> = mutableListOf()

        doDbCalls(individual.seeInitializingActions(), actionResults = actionResults)

        val fv = FitnessValue(individual.size().toDouble())

        run loop@{
            individual.seeActions().forEachIndexed { index, action->
                val dto = convertor.transformActionDto(action, index)
                val ok = rc.executeNewRPCAction(dto, actionResults)
                if (!ok) return@loop
            }
        }

        val dto = updateFitnessAfterEvaluation(targets, individual, fv)
                ?: return null
        handleExtra(dto, fv)

        /*
            TODO Man handle targets regarding info in responses,
            eg, exception
                there is no specific status info in response for thrift,
                    then we might support customized property as we mentioned in industry paper (optional)
                status info in GRPC, see https://grpc.github.io/grpc/core/md_doc_statuscodes.html
         */
        handleResponseTargets()

        if (config.baseTaintAnalysisProbability > 0) {
            Lazy.assert { actionResults.size == dto.additionalInfoList.size }
            TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness)
        }

        return EvaluatedIndividual(fv, individual.copy() as RPCIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)

    }

    private fun handleResponseTargets(){
        // TODO
    }

}