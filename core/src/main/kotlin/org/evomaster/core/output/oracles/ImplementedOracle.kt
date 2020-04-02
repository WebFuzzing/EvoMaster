package org.evomaster.core.output.oracles

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.ObjectGenerator
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.EvaluatedIndividual

abstract class ImplementedOracle {
    abstract fun variableDeclaration(lines: Lines, format: OutputFormat)
    abstract fun addExpectations(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat)
    abstract fun setObjectGenerator(gen: ObjectGenerator)
    abstract fun generatesExpectation(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat): Boolean
    abstract fun selectForClustering(action: EvaluatedAction): Boolean
}