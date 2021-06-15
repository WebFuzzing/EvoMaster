package org.evomaster.core.output.service

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.Lines
import org.evomaster.core.output.formatter.MismatchedFormatException
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQLUtils
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.GeneUtils
import org.slf4j.LoggerFactory
import javax.ws.rs.core.MediaType

class GraphQLTestCaseWriter : HttpWsTestCaseWriter() {

    companion object {
        private val log = LoggerFactory.getLogger(GraphQLTestCaseWriter::class.java)
    }

    override fun handleActionCalls(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>){
        if (ind.individual is GraphQLIndividual) {
            ind.evaluatedActions().forEach { a ->
                handleGraphQlCall(a, lines, baseUrlOfSut)
            }
        }
    }

    override fun addActionLines(action: Action, lines: Lines, result: ActionResult, baseUrlOfSut: String) {
        addGraphQlCallLines(action as GraphQLAction, lines, result as GraphQlCallResult, baseUrlOfSut)
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


    private fun handleGQLFirstLine(call: GraphQLAction, lines: Lines, res: GraphQlCallResult) {

        lines.addEmpty()

        when {
            format.isJavaOrKotlin() -> lines.append("given()")
            format.isJavaScript() -> lines.append("await superagent")
        }

        if (!format.isJavaScript()) {
            lines.append(getAcceptHeader(call, res))
        }
    }

    private fun handleGQLLastLine(lines: Lines) {

        lines.appendSemicolon(format)
        lines.deindent(2)
    }

    private fun handleGQLResponse(call: GraphQLAction, res: GraphQlCallResult, lines: Lines) {
        if (!res.failedCall()) {

            val code = res.getStatusCode()

            when {
                format.isJavaOrKotlin() -> {
                    lines.add(".then()")
                    lines.add(".statusCode($code)")
                }
                // This does not work in Superagent. TODO will need after the HTTP call
                //format.isJavaScript() -> lines.add(".expect($code)")
            }

            var commented = false
            if (code == 500) {
                commented = true
                lines.append(" // " + res.getLastStatementWhen500())
            }

            //TODO Man: shall we add lastStatement with errors here?
            if (res.getLastStatementWhenGQLErrors() != null) {
                lines.append("${if (!commented) "//" else ","} errors:${res.getLastStatementWhenGQLErrors()}")
            }

            if (config.enableBasicAssertions) {
                handleGQLResponseContents(lines, res)
            }
        }
    }

    private fun handleGraphQlCall(
            evaluatedAction: EvaluatedAction,
            lines: Lines,
            baseUrlOfSut: String
    ) {
        lines.addEmpty()
        val call = evaluatedAction.action as GraphQLAction
        val res = evaluatedAction.result as GraphQlCallResult

        if (res.failedCall() || format.isJavaScript() //looks like even 400 throws exception with SuperAgent... :(
        ) {
            addActionInTryCatch(call, lines, res, baseUrlOfSut)
        } else {
            addGraphQlCallLines(call, lines, res, baseUrlOfSut)
        }
    }


    private fun addGraphQlCallLines(call: GraphQLAction,
                                    lines: Lines,
                                    res: GraphQlCallResult,
                                    baseUrlOfSut: String) {

        handleGQLFirstLine(call, lines, res)

        lines.indent(2)

        when {
            format.isJavaOrKotlin() -> {
                handleHeaders(call, lines)
                handleGQLBody(call, lines)
                handleGQLVerb(baseUrlOfSut, call, lines)
            }
            format.isJavaScript() -> {
                //in SuperAgent, verb must be first
                handleGQLVerb(baseUrlOfSut, call, lines)
                lines.append(getAcceptHeader(call, res))
                handleHeaders(call, lines)
                handleGQLBody(call, lines)
            }
        }

        handleGQLResponse(call, res, lines)
        handleGQLLastLine(lines)
    }

    private fun handleGQLVerb(baseUrlOfSut: String, call: GraphQLAction, lines: Lines) {

        // TODO maybe in future might want to have GET for QUERY types
        val verb = "post"
        lines.add(".$verb(")

        if (format.isKotlin()) {
            lines.append("\"\${$baseUrlOfSut}")
        } else {
            lines.append("$baseUrlOfSut + \"")
        }
        val path = "/graphql"
        lines.append("${GeneUtils.applyEscapes(path, mode = GeneUtils.EscapeMode.NONE, format = format)}\"")
        lines.append(")")
    }

    /**
     * response are handled with 'data'
     */
    private fun handleGQLResponseContents(lines: Lines, res: GraphQlCallResult) {

        if (format.isJavaScript()) {
            //TODO
            return
        }

        lines.add(".assertThat()")

        if (res.getBodyType() == null) {
//            lines.add(".contentType(\"\")")
//            if (res.getBody().isNullOrBlank() && res.getStatusCode() != 400) lines.add(".body(isEmptyOrNullString())")
            lines.add(".body(isEmptyOrNullString())")
        } else lines.add(".contentType(\"${res.getBodyType()
                .toString()
                .split(";").first() //TODO this is somewhat unpleasant. A more elegant solution is needed.
        }\")")


        if (res.getBodyType() != null) {
            val bodyString = res.getBody()

            val type = res.getBodyType()!!
            if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().toLowerCase().contains("+json"))
            {
                when (bodyString?.trim()?.first()) {
                    //TODO, Man: need a check with Asma or Anrea, it seems never be true in GraphQL, shall we delete this option?
                    '[' -> {
                        // This would be run if the JSON contains an array of objects.
                        val resContents = Gson().fromJson(bodyString, ArrayList::class.java)
                        lines.add(".body(\"size()\", equalTo(${resContents.size}))")
                        //assertions on contents
                        if (resContents.size > 0) {
                            var longArray = false
                            resContents.forEachIndexed { test_index, value ->
                                when {
                                    (value is Map<*, *>) -> handleMapLines(test_index, value, lines)
                                    (value is String) -> longArray = true
                                    else -> {
                                        val printableFieldValue = handleFieldValues_getMatcher(value)
                                        if (isSuitableToPrint(printableFieldValue)) {
                                            lines.add(".body(\"\", $printableFieldValue)")
                                        }
                                    }
                                }
                            }
                            if (longArray) {
                                val printableContent = handleFieldValues_getMatcher(resContents)
                                if (isSuitableToPrint(printableContent)) {
                                    lines.add(".body(\"\", $printableContent)")
                                }
                            }
                        } else {
                            // the object is empty
                            if (format.isKotlin()) lines.add(".body(\"isEmpty()\", `is`(true))")
                            else lines.add(".body(\"isEmpty()\", is(true))")
                        }
                    }
                    '{' -> {
                        // JSON contains an object
                        val resContents = Gson().fromJson(bodyString, Map::class.java)
                        handleAssertionsOnObject(resContents, lines, null)

                    }
                    else -> {
                        // This branch will be called if the JSON is null (or has a basic type)
                        // Currently, it converts the contents to String.
                        when {
                            res.getTooLargeBody() -> lines.add("/* very large body, which was not handled during the search */")

                            bodyString.isNullOrBlank() -> lines.add(".body(isEmptyOrNullString())")

                            else -> lines.add(".body(containsString(\"${
                                GeneUtils.applyEscapes(bodyString, mode = GeneUtils.EscapeMode.BODY, format = format)
                            }\"))")
                        }
                    }
                }
            }
        }
    }

    private fun handleGQLBody(call: GraphQLAction, lines: Lines) {

        val send = when {
            format.isJavaOrKotlin() -> "body"
            format.isJavaScript() -> "send"
            else -> throw IllegalArgumentException("Format not supported $format")
        }

        when {
            format.isJavaOrKotlin() -> lines.add(".contentType(\"application/json\")")
            format.isJavaScript() -> lines.add(".set('Content-Type','application/json')")

        }

        val bodyEntity = GraphQLUtils.generateGQLBodyEntity(call, format)

        val body = if (bodyEntity!=null) {
            try{
                OutputFormatter.JSON_FORMATTER.getFormatted(bodyEntity.entity)
            }catch (e: MismatchedFormatException){
                LoggingUtil.uniqueWarn(log, e.message?:"failed to format ${bodyEntity.entity}")
                bodyEntity.entity
            }
        } else {
            LoggingUtil.uniqueWarn(log, " method type not supported yet : ${call.methodType}").toString()
        }


        //needed as JSON uses ""
        val bodyLines = body.split("\n").map { s ->
            "\" " + GeneUtils.applyEscapes(s.trim(), mode = GeneUtils.EscapeMode.BODY, format = format) + " \""
        }

        if (bodyLines.size == 1) {
            lines.add(".$send(${bodyLines.first()})")
        } else {
            lines.add(".$send(${bodyLines.first()} + ")
            lines.indented {
                (1 until bodyLines.lastIndex).forEach { i ->
                    lines.add("${bodyLines[i]} + ")
                }
                lines.add("${bodyLines.last()})")
            }
        }
    }
}