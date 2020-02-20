package org.evomaster.core.output.oracles

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.service.ObjectGenerator
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.EvaluatedAction

class SchemaOracle : ImplementedOracle() {
    private val variableName = "sch"
    private lateinit var objectGenerator: ObjectGenerator

    override fun variableDeclaration(lines: Lines, format: OutputFormat) {
        lines.add("/**")
        lines.add("* $variableName - supported code oracle - checking that the response status code is among those supported according to the schema")
        lines.add("*/")
        when{
            format.isJava() -> {
                lines.add("private static boolean $variableName = false;")
            }
            format.isKotlin() -> {
                lines.add("private val $variableName = false")
            }
        }
    }

    override fun addExpectations(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getSupportedResponse(call: RestCallAction): MutableSet<String>{
        val verb = call.verb
        val path = objectGenerator.getSwagger().paths.get(call.path.toString())
        val specificPath = when(verb){
            HttpVerb.GET -> path?.get
            HttpVerb.POST -> path?.post
            HttpVerb.PUT -> path?.put
            HttpVerb.DELETE -> path?.delete
            HttpVerb.PATCH -> path?.patch
            HttpVerb.HEAD -> path?.head
            HttpVerb.OPTIONS -> path?.options
            HttpVerb.TRACE -> path?.trace
            else -> null
        }
        val result = specificPath?.responses?.values?.flatMap { va -> va.content.values.map { it.schema.type } }?.toMutableSet() ?: mutableSetOf()
        return result
    }

    override fun setObjectGenerator(gen: ObjectGenerator) {
        objectGenerator = gen
    }

    override fun generatesExpectation(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun selectForClustering(action: EvaluatedAction): Boolean {

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}