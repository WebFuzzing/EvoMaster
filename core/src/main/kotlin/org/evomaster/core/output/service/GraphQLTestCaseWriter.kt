package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.core.output.Lines
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQLUtils
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.graphql.service.GraphQLFitness
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.LoggerFactory
import java.nio.file.Path

class GraphQLTestCaseWriter : HttpWsTestCaseWriter() {

    companion object {
        private val log = LoggerFactory.getLogger(GraphQLTestCaseWriter::class.java)
    }

    @Inject
    protected lateinit var fitness: GraphQLFitness

    override fun handleActionCalls(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>, insertionVars: MutableList<Pair<String, String>>, testCaseName: String, testSuitePath: Path?){
        if (ind.individual is GraphQLIndividual) {
            ind.evaluatedMainActions().forEachIndexed { index,  a ->
                handleSingleCall(a, index, ind.fitness, lines, testCaseName, testSuitePath, baseUrlOfSut)
            }
        }
    }

    override fun addActionLines(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String) {
        addGraphQlCallLines(action as GraphQLAction, lines, result as GraphQlCallResult, baseUrlOfSut)
    }

    private fun addGraphQlCallLines(call: GraphQLAction, lines: Lines, result: GraphQlCallResult, baseUrlOfSut: String) {

        val responseVariableName = makeHttpCall(call, lines, result, baseUrlOfSut)
        handleResponseAfterTheCall(call, result, responseVariableName, lines)
    }

    override fun handleBody(call: HttpWsAction, lines: Lines) {

        /*
            TODO: when/if we are going to deal with GET, then we will need to update/refactor this code
         */

        when {
            format.isJavaOrKotlin() -> lines.add(".contentType(\"application/json\")")
            format.isJavaScript() -> lines.add(".set('Content-Type','application/json')")
           // format.isCsharp() -> lines.add("Client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue(\"application/json\"));")
        }


        val gql = call as GraphQLAction

        val body = GraphQLUtils.generateGQLBodyEntity(gql, format)
        printSendJsonBody(body!!.entity, lines)
    }

    override fun getAcceptHeader(call: HttpWsAction, res: HttpWsCallResult): String {

        val accept = openAcceptHeader()

        /**
         * GQL services typically respond using JSON
         */
        var result =  "$accept\"application/json\""
        result = closeAcceptHeader(result)
        return result
    }


    override fun handleLastStatementComment(res: HttpWsCallResult, lines: Lines){

        super.handleLastStatementComment(res, lines)

        val code = res.getStatusCode()

        /*
            if last line has already been added due to 500, no point in adding again
         */

        val gql = res as GraphQlCallResult

        if (code != 500 && gql.hasLastStatementWhenGQLError()) {
            lines.append(" // " + gql.getLastStatementWhenGQLErrors())
        }
    }

    override fun handleVerbEndpoint(baseUrlOfSut: String, _call: HttpWsAction, lines: Lines) {

        // TODO maybe in future might want to have GET for QUERY types
        val verb = "post"
        lines.add(".$verb(")

        if(config.blackBox){
            /*
                in BB, the baseUrl is actually the full endpoint
             */

            if (format.isKotlin()) {
                lines.append("\"\${$baseUrlOfSut}\"")
            } else {
                lines.append("$baseUrlOfSut")
            }
        } else {

            if (format.isKotlin()) {
                lines.append("\"\${$baseUrlOfSut}")
            } else {
                lines.append("$baseUrlOfSut + \"")
            }

           val path= fitness.infoDto.graphQLProblem.endpoint

              // infoDto.graphQLProblem?.endpoint

            lines.append("${path?.let { GeneUtils.applyEscapes(it, mode = GeneUtils.EscapeMode.NONE, format = format) }}\"")
        }

        lines.append(")")
    }

}
