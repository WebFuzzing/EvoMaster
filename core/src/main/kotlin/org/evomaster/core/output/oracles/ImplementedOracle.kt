package org.evomaster.core.output.oracles

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.service.ObjectGenerator
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult

abstract class ImplementedOracle {
    public abstract fun variableDeclaration(lines: Lines, format: OutputFormat)
    public abstract fun addExpectations(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat)
    public abstract fun setObjectGenerator(gen: ObjectGenerator)
    public abstract fun generatesExpectation(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat): Boolean
}