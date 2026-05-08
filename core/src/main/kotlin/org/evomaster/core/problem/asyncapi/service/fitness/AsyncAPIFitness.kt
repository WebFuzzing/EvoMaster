package org.evomaster.core.problem.asyncapi.service.fitness

import org.evomaster.core.problem.asyncapi.data.AsyncAPIIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.slf4j.LoggerFactory

/**
 * White-box AsyncAPI fitness.
 *
 * Inherits the full schema-derivable target set from [AbstractAsyncAPIFitness]
 * and layers on the JVM-coverage signals the EM Driver provides:
 *
 *  - Each action is registered with the driver via
 *    [org.evomaster.core.problem.enterprise.service.EnterpriseFitness.registerNewAction]
 *    just before publish/subscribe so the bytecode instrumentation attributes
 *    branch/line targets to it.
 *  - Once all actions in an individual have been processed, the driver is
 *    polled for the resulting branch/line coverage and the targets are
 *    merged into the fitness value.
 *
 * The reason this is more than a one-liner over black-box: the broker hop
 * between EvoMaster's publish call and the SUT's `@KafkaListener` callback
 * is asynchronous, so the driver's coverage poll happens once at the *end*
 * of the action loop rather than per-action.  M6 is expected to refine
 * this with a hybrid wait strategy (coverage stabilisation + per-action
 * timeout); the starter slice trusts the broker's at-least-once semantics
 * to have delivered every message before we ask for results.
 */
class AsyncAPIFitness : AbstractAsyncAPIFitness() {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncAPIFitness::class.java)
    }

    override fun beforeIndividualEvaluation() {
        // Reset the SUT so coverage is measured against a clean baseline on
        // every evaluation.  Without this, listener-side state (DB rows,
        // in-memory caches, retry queues) accumulates across individuals and
        // fitness measurements drift.  REST does the equivalent in
        // RestFitness; aligning here mirrors that contract.
        try {
            rc.resetSUT()
        } catch (e: Exception) {
            log.warn("rc.resetSUT() failed before AsyncAPI individual evaluation: {}", e.message)
        }
    }

    override fun beforeAction(action: org.evomaster.core.problem.asyncapi.data.AsyncAPIAction, index: Int) {
        // Tell the EM Driver which action's coverage is about to be exercised.
        registerNewAction(action, index)
    }

    override fun afterIndividualEvaluated(
        individual: AsyncAPIIndividual,
        fv: FitnessValue,
        actionResults: List<ActionResult>,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean
    ) {
        try {
            updateFitnessAfterEvaluation(
                targets = targets,
                allTargets = allTargets,
                fullyCovered = fullyCovered,
                descriptiveIds = descriptiveIds,
                individual = individual,
                fv = fv
            )
        } catch (e: Exception) {
            // Coverage polling can fail transiently when the driver is busy
            // restarting the SUT.  We don't want a single failed poll to
            // discard the whole evaluation, so just warn and rely on the
            // schema-derivable targets the abstract base already added.
            log.warn("Failed to fetch white-box coverage from EM Driver: {}", e.message)
        }
    }
}
