package org.evomaster.core.problem.asyncapi.service.fitness

import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
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
 *  - Before each action [registerNewAction] tells the driver which action's
 *    coverage is about to be exercised so attribution lands on the right index.
 *  - After a fire-and-forget PUBLISH, [awaitConsumerSettled] runs the hybrid
 *    coverage-stabilisation strategy from the plan / Thesis §7: poll
 *    `getTestResults` every [EMConfig.asyncApiCoverageStabilisationPollMs]
 *    until the covered-target count hasn't changed for
 *    [EMConfig.asyncApiCoverageStabilisationWindowMs], capped at
 *    [EMConfig.asyncApiCoverageStabilisationMaxMs].  The simple settle from
 *    the abstract base remains as a lower bound — gives the driver time to
 *    receive at least one batch of targets before polling can declare
 *    "stable".
 *  - At end-of-individual the driver is polled for the resulting branch/line
 *    coverage and those targets are merged into the fitness value.
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

    override fun beforeAction(action: AsyncAPIAction, index: Int) {
        // Tell the EM Driver which action's coverage is about to be exercised.
        registerNewAction(action, index)
    }

    override fun awaitConsumerSettled(action: AsyncAPIAction, index: Int) {
        // Lower-bound floor: let the driver receive at least one batch of
        // targets before we start asking "is anything still arriving?".  This
        // also doubles as the fallback for runs where getTestResults fails
        // (the catch in the loop below logs and breaks out without raising).
        applyFireAndForgetSettle()

        val pollMs = config.asyncApiCoverageStabilisationPollMs.toLong()
        val windowMs = config.asyncApiCoverageStabilisationWindowMs.toLong()
        val maxMs = config.asyncApiCoverageStabilisationMaxMs.toLong()

        val deadline = System.currentTimeMillis() + maxMs
        var lastTargetCount = sampleCoveredTargetCount() ?: return  // driver unreachable; the settle was the best we could do
        var lastChange = System.currentTimeMillis()

        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(pollMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }

            val now = sampleCoveredTargetCount() ?: return
            if (now != lastTargetCount) {
                lastTargetCount = now
                lastChange = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - lastChange >= windowMs) {
                if (log.isTraceEnabled) {
                    log.trace(
                        "AsyncAPI fire-and-forget action #{} ({}) settled at {} targets",
                        index, action.getName(), now
                    )
                }
                return
            }
        }

        log.debug(
            "AsyncAPI fire-and-forget action #{} ({}) did not settle within {}ms; proceeding",
            index, action.getName(), maxMs
        )
    }

    /**
     * Cheap probe of the driver's current covered-target count.  We do not
     * keep the result — coverage is harvested in bulk at end-of-individual via
     * [updateFitnessAfterEvaluation].  Returns null on transient failure so
     * the caller can fall back to the simple settle without aborting.
     */
    private fun sampleCoveredTargetCount(): Int? {
        return try {
            rc.getTestResults(setOf(), ignoreKillSwitch = true)?.targets?.size
        } catch (e: Exception) {
            log.debug("Stabilisation poll skipped: {}", e.message)
            null
        }
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
