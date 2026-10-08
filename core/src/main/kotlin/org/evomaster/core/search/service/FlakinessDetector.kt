package org.evomaster.core.search.service

import com.google.inject.Inject
import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.service.time.ExecutionPhaseController
import org.evomaster.core.search.service.time.TimeBoxedPhase
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * it is used to detect flaky tests by checking responses or return value
 * currently, such a detection is performed during post-handling of fuzzing
 */
class FlakinessDetector<T: Individual> : TimeBoxedPhase {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FlakinessDetector::class.java)
    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var archive: Archive<T>

    @Inject
    private lateinit var fitness : FitnessFunction<T>

    @Inject
    private lateinit var epc: ExecutionPhaseController

    data class DetectionProgress(
        val expected: Long,
        var attempted: Long = 0,
        var completed: Long = 0,
        var partialComparisons: Long = 0,
        var exceptionalExecutions: Long = 0,
        var timedOut: Boolean = false
    ) {
        val failed: Long get() = attempted - completed
        val notAttempted: Long get() = expected - attempted
        val complete: Boolean get() = completed == expected && !timedOut
    }

    var staticProgress: DetectionProgress? = null
        private set
    var reexecutionProgress: DetectionProgress? = null
        private set

    var observationFile: Path? = null
        private set

    private fun saveObservation(individualIndex: Int, repetition: Int, baseline: List<ActionResult>,
                                plannedIds: List<String>, observed: List<ActionResult>, recorded: Boolean,
                                coverageAvailable: Boolean, error: String? = null) {
        fun snapshot(result: ActionResult) = mapOf(
            "actionId" to result.sourceLocalId, "type" to result.javaClass.name,
            "stopping" to result.stopping, "deathSentence" to result.deathSentence,
            "values" to result.resultValues())
        val ids = observed.map { it.sourceLocalId }.toSet()
        val entry = mapOf("schemaVersion" to 1, "individualIndex" to individualIndex,
            "repetition" to repetition, "recorded" to recorded, "coverageAvailable" to coverageAvailable,
            "error" to error, "plannedActionIds" to plannedIds,
            "notExecutedActionIds" to plannedIds.filter { it !in ids },
            "baseline" to baseline.map { snapshot(it) }, "observed" to observed.map { snapshot(it) })
        Files.writeString(observationFile!!, Gson().toJson(entry) + "\n", StandardOpenOption.APPEND)
    }

    override fun applyPhase() {
        staticProgress = null
        reexecutionProgress = null
        if (!config.handleFlakiness) {
            throw IllegalStateException("handleFlakiness must be enabled before applying this phase of flakiness detection and handing with FlakinessDetector")
        }

        if (config.enableStaticFlakyInference) {
            inferStaticFlakiness()
        }
        if (config.execNumForDetectFlakiness > 0) {
            reexecuteToDetectFlakiness(config.execNumForDetectFlakiness)
        }
    }

    override fun hasPhaseTimedOut(): Boolean {
        return config.useTimeLimitForFlakiness &&
                epc.hasPhaseTimedOut(ExecutionPhaseController.Phase.FLAKINESS)
    }

    private fun reportProgress(name: String, progress: DetectionProgress, started: Long) {
        val message = "Flakiness $name: expected=${progress.expected}, attempted=${progress.attempted}, " +
                "completed=${progress.completed}, failed=${progress.failed}, " +
                "notAttempted=${progress.notAttempted}, timedOut=${progress.timedOut}, " +
                "complete=${progress.complete}, elapsedMs=${(System.nanoTime() - started) / 1_000_000}"
        if (progress.complete) LoggingUtil.getInfoLogger().info(message)
        else LoggingUtil.getInfoLogger().warn(message)
        LoggingUtil.getInfoLogger().info("Flakiness $name observations: partialComparisons=${progress.partialComparisons}, exceptionalExecutions=${progress.exceptionalExecutions}")
    }

    /**
     * re-execute individuals in archive for identifying flakiness
     */
    fun reexecuteToDetectFlakiness(execNum : Int) : Solution<T>{
        require(execNum >= 0)
        /*
            here we rely on extractSolution returning actual references of individuals in the archive, and not copies
         */
        val currentIndividuals = archive.extractSolution().individuals
        val started = System.nanoTime()
        val progress = DetectionProgress(currentIndividuals.size.toLong() * execNum)
        reexecutionProgress = progress
        val output = Paths.get(config.outputFolder)
        Files.createDirectories(output)
        observationFile = Files.createTempFile(output, "flakiness-observations-", ".jsonl")
        LoggingUtil.getInfoLogger().info("Flakiness execution records: $observationFile")

        LoggingUtil.getInfoLogger().info("Reexecuting all individual ${currentIndividuals.size} for identifying flakiness.")

        try {
            detection@ for ((individualIndex, ci) in currentIndividuals.withIndex()) {
                val individualProgress = DetectionProgress(execNum.toLong())
                val individualStarted = System.nanoTime()
                try {
                    for (execIndex in 1..execNum) {
                        if (hasPhaseTimedOut()) {
                            progress.timedOut = true
                            individualProgress.timedOut = true
                            break@detection
                        }
                        progress.attempted++
                        individualProgress.attempted++
                        var observed: List<ActionResult> = emptyList()
                        var captured = false
                        val baseline = ci.evaluatedMainActions().map { it.result }
                        val plannedIds = ci.individual.seeMainExecutableActions().map { it.getLocalId() }
                        val ei = try {
                            fitness.computeWholeAchievedCoverageForPostProcessing(ci.individual) {
                                observed = it
                                captured = true
                            }
                        } catch (e: Exception) {
                            saveObservation(individualIndex, execIndex, baseline, plannedIds, observed, false, false, e.toString())
                            throw e
                        }
                        if (ei != null) observed = ei.evaluatedMainActions().map { it.result }
                        // Completion means a finished evaluation record, not a successful test or identical path.
                        val observedIds = observed.map { it.sourceLocalId }
                        val validIds = observedIds.distinct().size == observedIds.size
                        val finishedPath = plannedIds.isEmpty() ||
                                (observedIds == plannedIds.take(observedIds.size) &&
                                        (observedIds.size == plannedIds.size || observed.lastOrNull()?.stopping == true))
                        val recorded = (ei != null || (captured && observed.isNotEmpty())) && validIds && finishedPath
                        saveObservation(individualIndex, execIndex, baseline, plannedIds, observed, recorded, ei != null)
                        if (recorded) {
                            if (!compareObservedResults(observed, ci, execIndex)) {
                                progress.partialComparisons++
                                individualProgress.partialComparisons++
                            }
                            if (observed.any { it is HttpWsCallResult && it.failedCall() }) {
                                progress.exceptionalExecutions++
                                individualProgress.exceptionalExecutions++
                            }
                            progress.completed++
                            individualProgress.completed++
                        } else {
                            log.warn("Missing execution record for individual $individualIndex, repetition $execIndex during flakiness analysis.")
                        }
                    }
                } finally {
                    reportProgress("individual $individualIndex re-execution", individualProgress, individualStarted)
                }
            }
        } finally {
            // Keep the incomplete summary even if evaluation throws; do not suppress the exception.
            reportProgress("re-execution", progress, started)
        }

        return archive.extractSolution()
    }

    /**
     * Infer potential flaky response values without re-executing the SUT.
     */
    fun inferStaticFlakiness(): Solution<T> {
        val currentIndividuals = archive.extractSolution().individuals
        val started = System.nanoTime()
        val progress = DetectionProgress(currentIndividuals.size.toLong())
        staticProgress = progress

        LoggingUtil.getInfoLogger().info("Inferring static flakiness for ${currentIndividuals.size} individuals.")

        try {
            for (ci in currentIndividuals) {
                if(hasPhaseTimedOut()) {
                    progress.timedOut = true
                    break
                }
                progress.attempted++

                ci.evaluatedMainActions()
                    .filter { it.action is HttpWsAction && it.result is HttpWsCallResult }
                    .forEach {
                        (it.result as HttpWsCallResult).recordStaticFlakyInference()
                    }
                progress.completed++
            }
        } finally {
            reportProgress("static inference", progress, started)
        }

        return archive.extractSolution()
    }

    /**
     * This might have side-effects will be applied in the archive
     * compare [inArchive] with [other] to check if the action results are same, the inconsistent info will be saved in [inArchive] evaluated individual
     * @param inArchive the evaluated individual which saves info of flakiness
     * @param indexExecN the index of execution times, eg, 1st, 2nd
     */
    fun checkAndMarkConsistency(other: EvaluatedIndividual<T>, inArchive: EvaluatedIndividual<T>, indexExecN: Int): Boolean {
        return compareObservedResults(other.evaluatedMainActions().map { it.result }, inArchive, indexExecN)
    }

    private fun compareObservedResults(observed: List<ActionResult>, inArchive: EvaluatedIndividual<T>, indexExecN: Int): Boolean {
        val currentActions = inArchive.evaluatedMainActions()
        val baselineIds = currentActions.map { it.result.sourceLocalId }
        val observedIds = observed.map { it.sourceLocalId }
        // Ambiguous IDs must never cause a response to be assigned to the wrong action.
        if (baselineIds.distinct().size != baselineIds.size || observedIds.distinct().size != observedIds.size) {
            log.warn("Duplicate action IDs during flakiness comparison, repetition $indexExecN.")
            return false
        }
        val byId = observed.associateBy { it.sourceLocalId }
        var complete = baselineIds == observedIds
        if (!complete) {
            log.warn("Flakiness execution path differs, repetition $indexExecN: " +
                    "baselineIds=$baselineIds, observedIds=$observedIds, " +
                    "missingIds=${baselineIds - observedIds.toSet()}, extraIds=${observedIds - baselineIds.toSet()}")
        }
        currentActions.forEach { current ->
            val result = current.result
            if (current.action is HttpWsAction && result is HttpWsCallResult) {
                val other = byId[result.sourceLocalId]
                if (other is HttpWsCallResult) {
                    handleFlakinessInActionResult(result, other, indexExecN)
                    if (other.failedCall() || result.stopping != other.stopping) {
                        complete = false
                        log.warn("Flakiness incomplete action, repetition $indexExecN, actionId=${result.sourceLocalId}: " +
                                "tcpProblem=${other.getTcpProblem()}, timedOut=${other.getTimedout()}, " +
                                "baselineStopping=${result.stopping}, observedStopping=${other.stopping}")
                    }
                } else {
                    complete = false
                    log.warn("Missing or incompatible result during flakiness comparison, repetition $indexExecN, actionId=${result.sourceLocalId}.")
                }
            }
        }
        return complete
    }

    private fun handleFlakinessInActionResult(
        resultToUpdate: HttpWsCallResult,
        other: HttpWsCallResult,
        indexExecN: Int
    ) {
        resultToUpdate.recordFlakyObservation(other, indexExecN)
    }

}
