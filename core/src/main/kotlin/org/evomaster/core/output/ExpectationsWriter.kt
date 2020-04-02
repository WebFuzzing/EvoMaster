package org.evomaster.core.output

import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult

class ExpectationsWriter {
    private var format: OutputFormat = OutputFormat.JAVA_JUNIT_4
    private lateinit var swagger: OpenAPI
    private lateinit var partialOracles: PartialOracles
    //private val portRegex = """\w+:\d{4,5}""".toRegex()
    private val portRegex = """(\w+|(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})):\d{1,5}""".toRegex()

    fun setFormat(format: OutputFormat){
        this.format = format
    }

    fun setSwagger(swagger: OpenAPI){
        this.swagger = swagger
    }

    fun setPartialOracles(partialOracles: PartialOracles){
        this.partialOracles = partialOracles
    }

    fun addDeclarations(lines: Lines){
        lines.addEmpty()
        when{
            format.isJava() -> lines.append("ExpectationHandler expectationHandler = expectationHandler()")
            format.isKotlin() -> lines.append("val expectationHandler: ExpectationHandler = expectationHandler()")
        }
        lines.appendSemicolon(format)
    }

    fun handleExpectationSpecificLines(call: RestCallAction, lines: Lines, res: RestCallResult, name: String){
        lines.addEmpty()
        when{
            format.isKotlin() -> lines.add("val json_$name: JsonPath = $name")
            format.isJava() -> lines.add("JsonPath json_$name = $name")
        }

        lines.append(".extract().response().jsonPath()")
        lines.appendSemicolon(format)
        partialOracles.addExpectations(call, lines, res, name, format)
    }
}