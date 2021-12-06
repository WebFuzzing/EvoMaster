package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.core.Lazy
import org.evomaster.core.problem.httpws.service.HttpWsFitness
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.param.RPCReturnParam
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

    @Inject lateinit var rpcHandler: RPCEndpointsHandler

    override fun doCalculateCoverage(individual: RPCIndividual, targets: Set<Int>): EvaluatedIndividual<RPCIndividual>? {

        rc.resetSUT()

        // TOD handle auth
        val actionResults: MutableList<ActionResult> = mutableListOf()

        doDbCalls(individual.seeInitializingActions(), actionResults = actionResults)

        val fv = FitnessValue(individual.size().toDouble())

        run loop@{
            individual.seeActions().forEachIndexed { index, action->
                val ok = executeNewAction(action, index, actionResults)
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

    private fun executeNewAction(action: RPCCallAction, index: Int, actionResults: MutableList<ActionResult>) : Boolean{
        val actionResult = RPCCallResult()
        actionResults.add(actionResult)
        val dto = getActionDto(action, index)
        val rpc = rpcHandler.transformActionDto(action)
        dto.rpcCall = rpc

        val response =  rc.executeNewRPCActionAndGetResponse(dto)
        if (response != null){
            val responseGene = action.parameters.filterIsInstance<RPCReturnParam>().firstOrNull()
            if (responseGene != null)
                rpcHandler.setGeneBasedOnParamDto(responseGene.gene, response.rpcResponse)
        }

        actionResult.stopping = response != null
        return response != null
    }

    private fun handleResponseTargets(){
        // TODO
    }

}