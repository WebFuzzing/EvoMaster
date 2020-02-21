package org.evomaster.core.output.oracles

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.ObjectGenerator
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual

abstract class ImplementedOracle {
    public abstract fun variableDeclaration(lines: Lines, format: OutputFormat)
    public abstract fun addExpectations(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat)
    public abstract fun setObjectGenerator(gen: ObjectGenerator)
    public abstract fun generatesExpectation(call: RestCallAction, lines: Lines, res: RestCallResult, name: String, format: OutputFormat): Boolean
    public abstract fun selectForClustering(individual: EvaluatedIndividual<RestIndividual>): Boolean
}