package org.evomaster.core.output

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult

class ExpectationsHandler {
    private var format: OutputFormat = OutputFormat.JAVA_JUNIT_4

    fun setFormat(format: OutputFormat){
        this.format = format
    }

    fun addDeclarations(lines: Lines){
        lines.addEmpty()
        when{
            format.isJava() -> lines.append("ExpectationHandler expectationHandler = expectationHandler()")
            format.isKotlin() -> lines.append("val expectationHandler: ExpectationHandler = expectationHandler()")

        }
        lines.indented {
            lines.add(".expect(expectationsMasterSwitch)")
            if (format.isJava()) lines.append(";")
        }

    }

}