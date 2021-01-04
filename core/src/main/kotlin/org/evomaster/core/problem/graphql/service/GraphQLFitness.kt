package org.evomaster.core.problem.graphql.service

import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.httpws.service.HttpWsFitness
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.taint.TaintAnalysis
import javax.ws.rs.core.NewCookie

class GraphQLFitness : HttpWsFitness<GraphQLIndividual>() {


    override fun doCalculateCoverage(individual: GraphQLIndividual, targets: Set<Int>): EvaluatedIndividual<GraphQLIndividual>? {
        rc.resetSUT()

        val cookies = getCookies(individual)

        //TODO
        //doInitializingActions(individual)

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()


        //run the test, one action at a time
        for (i in 0 until individual.seeActions().size) {

            val a = individual.seeActions()[i]

            registerNewAction(a, i)

            var ok = false

            if (a is GraphQLAction) {
                ok = handleGraphQLCall(a, actionResults, cookies)
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }
        }

        //TODO
//        if(actionResults.any { it is RestCallResult && it.getTcpProblem() }){
//            /*
//                If there are socket issues, we avoid trying to compute any coverage.
//                The caller might restart the SUT and try again.
//                Hopefully, this should be just a glitch...
//                TODO if we see this happening often, we need to find a proper solution.
//                For example, we could re-run the test, and see if this one always fails,
//                while others in the archive do pass.
//                It could be handled specially in the archive.
//             */
//            return null
//        }

        val dto = updateFitnessAfterEvaluation(targets, individual, fv)
                ?: return null

        handleExtra(dto, fv)

        if (config.baseTaintAnalysisProbability > 0) {
            assert(actionResults.size == dto.additionalInfoList.size)
            TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness)
        }

        return EvaluatedIndividual(fv, individual.copy() as GraphQLIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)
    }

    private fun handleGraphQLCall(
            action: GraphQLAction,
            actionResults: MutableList<ActionResult>,
            cookies: Map<String, List<NewCookie>>
    ): Boolean {

        /*
            In GraphQL, there are 2 types of methods: Query and Mutation
            - Query can be on either a GET or POST HTTP call
            - Mutation must be on a POST (as changes are not idempotent)

            For simplicity, for now we just do POST for Query as well, as anyway URL have limitations
            on their length
         */

        //TODO
        return false
    }
}