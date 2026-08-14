package org.evomaster.core.search.service.minimizer

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.tracer.Traceable

class MultiActionFitness : FitnessFunction<MultiActionIndividual>() {

    //target id -> set of action ids that must ALL be present for the target to be covered
    var targetRequirements: Map<Int, Set<Int>> = emptyMap()

    //if set, this target is reported as covered only the first time a given individual (by its
    //set of action ids) is evaluated, simulating a flaky/non-deterministic SUT
    var flakyTarget: Int? = null
    private val evaluationCountByActionSet = mutableMapOf<Set<Int>, Int>()

    override fun doCalculateCoverage(
        individual: MultiActionIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
    ): EvaluatedIndividual<MultiActionIndividual>? {

        val fv = FitnessValue(individual.size().toDouble())

        val presentIds = individual.seeMainExecutableActions().map { it.id }.toSet()

        val evaluationCount = evaluationCountByActionSet.getOrDefault(presentIds, 0)
        evaluationCountByActionSet[presentIds] = evaluationCount + 1

        val toEvaluate = if (allTargets) targetRequirements.keys else targetsToEvaluate(targets, individual)

        toEvaluate.forEach { t ->
            val required = targetRequirements[t] ?: return@forEach
            val structurallyCovered = presentIds.containsAll(required)
            val covered = structurallyCovered && (t != flakyTarget || evaluationCount == 0)
            fv.updateTarget(t, if (covered) 1.0 else 0.0)
        }

        return EvaluatedIndividual(
            fv, individual.copy() as MultiActionIndividual,
            listOf(), config = config, trackOperator = individual.trackOperator,
            index = if (config.trackingEnabled()) time.evaluatedIndividuals else Traceable.DEFAULT_INDEX
        )
    }

    override fun targetsToEvaluate(targets: Set<Int>, individual: MultiActionIndividual): Set<Int> {
        return if (targets.isEmpty()) targetRequirements.keys else targets
    }
}
