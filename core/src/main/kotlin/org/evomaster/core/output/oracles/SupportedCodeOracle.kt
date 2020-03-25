package org.evomaster.core.output.oracles

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.ObjectGenerator
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
            format.isJava() -> lines.add("private static boolean $variableName = false;")
            format.isKotlin() -> lines.add("private val $variableName = false")
            format.isJavaScript() -> lines.add("const $variableName = false;")
        }

    }

    override fun addExpectations(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat) {
        if(!supportedCode(call, res)){
            // The code is not among supported codes, so an expectation will be generated
            //val actualCode = res.getStatusCode() ?: 0
            //lines.add(".that($oracleName, Arrays.asList(${getSupportedCode(call)}).contains($actualCode))")
            val supportedCode = getSupportedCode(call).joinToString(", ")
            if(supportedCode.equals("default", ignoreCase = true)){
                lines.add("/* Note: this call is handled via a default code. If this is intended behaviour, ignore this comment */")
            }
            else lines.add(".that($variableName, Arrays.asList($supportedCode).contains($name.extract().statusCode()))")
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
        return result?.responses?.keys ?: mutableSetOf()
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