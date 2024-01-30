package org.evomaster.core.output

import org.evomaster.core.TestUtils
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionResult
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.DbAsExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.RPCExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.parm.ClassResponseParam
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.param.RPCParam
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene

class EvaluatedIndividualBuilder {

    companion object {
        fun buildEvaluatedIndividual(dbInitialization: MutableList<SqlAction>): Triple<OutputFormat, String, EvaluatedIndividual<RestIndividual>> {
            val format = OutputFormat.JAVA_JUNIT_4

            val baseUrlOfSut = "baseUrlOfSut"

            val sampleType = SampleType.RANDOM

            val restActions = emptyList<RestCallAction>().toMutableList()

            val individual = RestIndividual(restActions, sampleType, dbInitialization)
            TestUtils.doInitializeIndividualForTesting(individual)

            val fitnessVal = FitnessValue(0.0)

            val ei = EvaluatedIndividual(fitnessVal, individual, generateIndividualResults(individual))
            return Triple(format, baseUrlOfSut, ei)
        }

        fun generateIndividualResults(individual: Individual) : List<ActionResult> =
            individual.seeActions(ActionFilter.ALL).map {
                if (it is SqlAction) SqlActionResult(it.getLocalId()).also { it.setInsertExecutionResult(true) }
                else ActionResult(it.getLocalId())
        }

        fun buildResourceEvaluatedIndividual(
            dbInitialization: MutableList<SqlAction>,
            groups: MutableList<Pair<MutableList<SqlAction>, MutableList<RestCallAction>>>,
            format: OutputFormat = OutputFormat.JAVA_JUNIT_4
        ): Triple<OutputFormat, String, EvaluatedIndividual<RestIndividual>> {

            val baseUrlOfSut = "baseUrlOfSut"
            val sampleType = SampleType.SMART_RESOURCE

            val calls = groups.map {
                RestResourceCalls(null, null, it.second, it.first)
            }.toMutableList()

            val individual = RestIndividual(calls, sampleType, null, dbInitialization)
            TestUtils.doInitializeIndividualForTesting(individual)

            val res = dbInitialization.map { SqlActionResult(it.getLocalId()).also { it.setInsertExecutionResult(true) } }
                    .plus(groups.flatMap { g->
                        g.first.map { SqlActionResult(it.getLocalId()).also { it.setInsertExecutionResult(true) } }
                            .plus(g.second.map { RestCallResult(it.getLocalId()).also { it.setTimedout(true) } })
                    }
                    )

            val fitnessVal = FitnessValue(0.0)

            val ei = EvaluatedIndividual(fitnessVal, individual, res)
            return Triple(format, baseUrlOfSut, ei)
        }

        fun buildFakeRPCAction(n:Int, interfaceId : String = "FakeRPCCall") : MutableList<RPCCallAction>{
            return (0 until n).map { RPCCallAction(interfaceId,"${interfaceId}_$it",
                inputParameters = mutableListOf(),
                responseTemplate= null,
                response = RPCParam("return", OptionalGene("return", StringGene("return")))
            ) }.toMutableList()
        }

        fun buildFakeRPCExternalServiceAction(n : Int): List<RPCExternalServiceAction>{
            return (0 until n).map {

                RPCExternalServiceAction(
                    interfaceName = "FakeRPC_Foo",
                    functionName = "foo",
                    inputParamTypes = null,
                    requestRuleIdentifier = null,
                    responseParam = ClassResponseParam(
                        className = "FakeRPCReturnDto",
                        responseType = EnumGene("responseType", listOf("JSON")),
                        response = OptionalGene("return",
                            ObjectGene("return", fields = listOf(StringGene("fakeMsg", "This is a fake response from a RPC-based external service"))) )
                    ),
                    active = true,
                    used = true
                )
            }
        }

        fun buildFakeDbExternalServiceAction(n : Int): List<DbAsExternalServiceAction>{
            return (0 until n).map {

                DbAsExternalServiceAction(
                    descriptiveInfo = "FakeDB_bar",
                    commandName = "bar",
                    requestRuleIdentifier = null,
                    responseParam = ClassResponseParam(
                        className = "FakeDbReturnDto",
                        responseType = EnumGene("responseType", listOf("JSON")),
                        response = OptionalGene("return",
                            ObjectGene("return", fields = listOf(StringGene("fakeMsg", "This is a fake response from a RPC-based external service"))) )
                    ),
                    active = true,
                    used = true
                )
            }
        }

        fun buildEvaluatedRPCIndividual(
            actions: MutableList<RPCCallAction>,
            externalServicesActions: MutableList<List<ApiExternalServiceAction>>,
            format: OutputFormat
        ): EvaluatedIndividual<RPCIndividual>{
            if (!format.isJavaOrKotlin())
                throw IllegalArgumentException("do not support to generate faked evaluated RPC individual for testing test writer")
            val individual = RPCIndividual(SampleType.RANDOM, actions = actions, externalServicesActions = externalServicesActions)

            TestUtils.doInitializeIndividualForTesting(individual)

            val fitnessVal = FitnessValue(0.0)

            return EvaluatedIndividual(fitnessVal, individual, actions.mapIndexed{i,a->
                RPCCallResult(a.getLocalId()).also {
                    it.setSuccess()
                    it.setHandledResponse(true)
                    it.setTestScript(listOf(if (format.isJava()) "int res_$i = 42;" else "val res_$i = 42"))
                    it.setResponseVariableName("res_$i")
                    it.setAssertionScript(listOf("assertEquals(42, res_$i)${if (format.isJava()) ";" else ""}"))
                }
            })
        }
    }
}
