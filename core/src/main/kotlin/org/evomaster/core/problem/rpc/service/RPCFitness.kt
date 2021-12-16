package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionType
import org.evomaster.core.Lazy
import org.evomaster.core.problem.api.service.ApiWsFitness
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.problem.rpc.RPCCallResultCategory
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
class RPCFitness : ApiWsFitness<RPCIndividual>() {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(RPCFitness::class.java)

        const val RPC_DEFAULT_FAULT_CODE = "RPC_framework_code"
    }

    @Inject lateinit var rpcHandler: RPCEndpointsHandler

    override fun doCalculateCoverage(individual: RPCIndividual, targets: Set<Int>): EvaluatedIndividual<RPCIndividual>? {

        rc.resetSUT()

        // TODO handle auth
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
        handleResponseTargets(fv, individual.seeActions(), actionResults.filterIsInstance<RPCCallResult>(), dto.additionalInfoList)

        if (config.baseTaintAnalysisProbability > 0) {
            Lazy.assert { actionResults.size == dto.additionalInfoList.size }
            TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness)
        }

        return EvaluatedIndividual(fv, individual.copy() as RPCIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)

    }

    private fun executeNewAction(action: RPCCallAction, index: Int, actionResults: MutableList<ActionResult>) : Boolean{

        // need for RPC as well
        searchTimeController.waitForRateLimiter()

        val actionResult = RPCCallResult()
        actionResults.add(actionResult)
        val dto = getActionDto(action, index)
        val rpc = rpcHandler.transformActionDto(action)
        dto.rpcCall = rpc

        val response =  rc.executeNewRPCActionAndGetResponse(dto)
        if (response != null){
            // check exception
            if (response.exceptionInfoDto != null){
                actionResult.setRPCException(response.exceptionInfoDto)
                if (response.exceptionInfoDto.type == RPCExceptionType.CUSTOMIZED_EXCEPTION){
                    if (response.exceptionInfoDto.exceptionDto!=null){
                        actionResult.setCustomizedExceptionBody(rpcHandler.getParamDtoJson(response.exceptionInfoDto.exceptionDto))
                    } else
                        log.warn("ERROR: missing customized exception dto")
                }
            } else{
                actionResult.setSuccess()
            }

            // check response
            if (response.rpcResponse !=null){
                Lazy.assert { action.responseTemplate != null }
                val responseParam = action.responseTemplate!!.copyContent()
                rpcHandler.setGeneBasedOnParamDto(responseParam.gene, response.rpcResponse)
                action.response = responseParam
            }
        }else{
            actionResult.setFailedCall()
        }

        actionResult.stopping = response == null

        return response != null
    }

    private fun handleResponseTargets(fv: FitnessValue,
                                      actions: List<RPCCallAction>,
                                      actionResults: List<RPCCallResult>,
                                      additionalInfoList: List<AdditionalInfoDto>
    ){
        actionResults.indices.forEach { i->
            val last = additionalInfoList[i].lastExecutedStatement?:RPC_DEFAULT_FAULT_CODE
            actionResults[i].getInvocationCode()
                ?:throw IllegalStateException("INVOCATION CODE is not assigned on the RPC call result")
            val category = RPCCallResultCategory.valueOf(actionResults[i].getInvocationCode()!!)
            handleAdditionalResponseTargetDescription(fv, category, actions[i].getName(), i, last)

            if (category ==RPCCallResultCategory.INTERNAL_ERROR){
                actionResults[i].setLastStatementForInternalError(last)
            }
        }
    }


    private fun handleAdditionalResponseTargetDescription(fv: FitnessValue,
                                                          category: RPCCallResultCategory,
                                                          name: String,
                                                          indexOfAction : Int,
                                                          locationPotentialBug: String){
        val okId = idMapper.handleLocalTarget("RPC_SUCCESS:$name")
        val failId = idMapper.handleLocalTarget("RPC_FAIL:$name")

        when(category){
            RPCCallResultCategory.SUCCESS->{
                fv.updateTarget(okId, 1.0, indexOfAction)
                fv.updateTarget(failId, 0.5, indexOfAction)
            }
            // shall we distinguish create additional targets for each kind of exception thrown
            RPCCallResultCategory.EXCEPTION, RPCCallResultCategory.CUSTOM_EXCEPTION ->{
                fv.updateTarget(okId, 0.1, indexOfAction)
                fv.updateTarget(failId, 0.1, indexOfAction)
            }

            RPCCallResultCategory.INTERNAL_ERROR, RPCCallResultCategory.UNEXPECTED_EXCEPTION->{
                fv.updateTarget(okId, 0.5, indexOfAction)
                fv.updateTarget(failId, 1.0, indexOfAction)

                val postfix = "$locationPotentialBug $name"
                val descriptiveId = if (category == RPCCallResultCategory.INTERNAL_ERROR)
                                        idMapper.getFaultDescriptiveIdForInternalError(postfix)
                                    else
                                        idMapper.getFaultDescriptiveIdForUnexpectedException(postfix)

                val bugId = idMapper.handleLocalTarget(descriptiveId)
                fv.updateTarget(bugId, 1.0, indexOfAction)
            }
            RPCCallResultCategory.FAILED->{
                // do nothing for the moment
            }
        }
    }

}