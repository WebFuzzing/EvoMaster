package org.evomaster.core.output.oracles

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.service.ObjectGenerator
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.EvaluatedIndividual

/**
 * The [SupportedCodeOracle] class generates an expectation and writes it to the code.
 *
 * A comparison is made between the status code of the [RestCallResult] and the supported return codes as defined
 * by the schema. If the actual code is not supported by the schema, the relevant expectation is generated and added
 * to the code.
 */

class SupportedCodeOracle : ImplementedOracle() {
    private val variableName = "sco"
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
        if(!supportedCode(call, res)){
            // The code is not among supported codes, so an expectation will be generated
            //val actualCode = res.getStatusCode() ?: 0
            //lines.add(".that($oracleName, Arrays.asList(${getSupportedCode(call)}).contains($actualCode))")
            lines.add(".that($variableName, Arrays.asList(${getSupportedCode(call).joinToString(", ")}).contains($name.extract().statusCode()))")
        }
    }
    fun supportedCode(call: RestCallAction, res: RestCallResult): Boolean{
        val code = res.getStatusCode().toString()
        val validCodes = getSupportedCode(call)
        return validCodes.contains(code)
    }

    fun getSupportedCode(call: RestCallAction): MutableSet<String>{
        val verb = call.verb
        val path = objectGenerator.getSwagger().paths.get(call.path.toString())
        val result = when (verb){
            HttpVerb.GET -> path?.get?.responses?.keys ?: mutableSetOf()
            HttpVerb.POST -> path?.post?.responses?.keys ?: mutableSetOf()
            HttpVerb.PUT -> path?.put?.responses?.keys ?: mutableSetOf()
            HttpVerb.DELETE -> path?.delete?.responses?.keys ?: mutableSetOf()
            HttpVerb.PATCH -> path?.patch?.responses?.keys ?: mutableSetOf()
            HttpVerb.HEAD -> path?.head?.responses?.keys ?: mutableSetOf()
            HttpVerb.OPTIONS -> path?.options?.responses?.keys ?: mutableSetOf()
            HttpVerb.TRACE -> path?.trace?.responses?.keys ?: mutableSetOf()
            else -> mutableSetOf()
        }
        return result
    }

    override fun setObjectGenerator(gen: ObjectGenerator){
        objectGenerator = gen
    }

    override fun generatesExpectation(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat): Boolean {
        return !supportedCode(call, res)
    }

    override fun selectForClustering(action: EvaluatedAction): Boolean {
        return if (action.result is RestCallResult && action.action is RestCallAction)
            !supportedCode(action.action, action.result)
        else false
    }
}