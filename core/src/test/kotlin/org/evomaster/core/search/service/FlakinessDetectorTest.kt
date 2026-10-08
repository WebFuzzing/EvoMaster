package org.evomaster.core.search.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.service.time.ExecutionPhaseController
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files
import com.google.gson.JsonParser

class FlakinessDetectorTest {
    @TempDir
    lateinit var output: Path

    @Test
    fun `timeout with explicit stop records unexecuted steps and completes every requested attempt`() {
        val f = fixture()
        val a = mockk<HttpWsAction>()
        val b = mockk<HttpWsAction>()
        every { a.getLocalId() } returns "a"
        every { b.getLocalId() } returns "b"
        every { f.individual.seeMainExecutableActions() } returns listOf(a, b)
        every { f.evaluated.evaluatedMainActions() } returns listOf(
            evaluatedAction(RestCallResult("a", false)), evaluatedAction(RestCallResult("b", false)))
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } answers {
            secondArg<((List<ActionResult>) -> Unit)?>()!!.invoke(listOf(
                RestCallResult("a", true).apply { setTimedout(true) }))
            null
        }
        f.detector.reexecuteToDetectFlakiness(2)
        assertTrue(f.detector.reexecutionProgress!!.complete)
        assertEquals(2L, f.detector.reexecutionProgress!!.exceptionalExecutions)
        val records = Files.readAllLines(f.detector.observationFile!!).map { JsonParser.parseString(it).asJsonObject }
        assertEquals(2, records.size)
        assertEquals(listOf(1, 2), records.map { it["repetition"].asInt })
        assertTrue(records.all { it["recorded"].asBoolean })
        assertTrue(records.all { it["notExecutedActionIds"].asJsonArray.single().asString == "b" })
    }

    @Test
    fun `truncated observation without stopping evidence does not pass`() {
        val f = fixture()
        val a = mockk<HttpWsAction>()
        val b = mockk<HttpWsAction>()
        every { a.getLocalId() } returns "a"
        every { b.getLocalId() } returns "b"
        every { f.individual.seeMainExecutableActions() } returns listOf(a, b)
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } answers {
            secondArg<((List<ActionResult>) -> Unit)?>()!!.invoke(listOf(RestCallResult("a", false)))
            null
        }
        f.detector.reexecuteToDetectFlakiness(1)
        assertFalse(f.detector.reexecutionProgress!!.complete)
        val record = JsonParser.parseString(Files.readString(f.detector.observationFile!!)).asJsonObject
        assertFalse(record["recorded"].asBoolean)
    }

    @Test
    fun `duplicate result IDs cannot pass the recording gate`() {
        val f = fixture()
        val other = mockk<EvaluatedIndividual<Individual>>()
        every { other.evaluatedMainActions() } returns listOf(
            evaluatedAction(RestCallResult("a", false)), evaluatedAction(RestCallResult("a", false)))
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } returns other
        f.detector.reexecuteToDetectFlakiness(1)
        assertEquals(1L, f.detector.reexecutionProgress!!.failed)
        assertFalse(f.detector.reexecutionProgress!!.complete)
    }
    private fun evaluatedAction(result: ActionResult): EvaluatedAction = mockk<EvaluatedAction>().also {
        every { it.action } returns mockk<HttpWsAction>()
        every { it.result } returns result
    }

    @Test
    fun `shorter execution still compares common actions without passing the gate`() {
        val f = fixture()
        val first = RestCallResult("a", false).apply { setBody("old") }
        every { f.evaluated.evaluatedMainActions() } returns listOf(
            evaluatedAction(first), evaluatedAction(RestCallResult("b", false)))
        val other = mockk<EvaluatedIndividual<Individual>>()
        every { other.evaluatedMainActions() } returns listOf(
            evaluatedAction(RestCallResult("a", true).apply { setBody("new") }))
        assertFalse(f.detector.checkAndMarkConsistency(other, f.evaluated, 1))
        assertNotNull(first.getFlakyObservation(1))
    }

    @Test
    fun `longer and reordered executions match by ID not position`() {
        val f = fixture()
        val first = RestCallResult("a", false).apply { setBody("a") }
        val second = RestCallResult("b", false).apply { setBody("b") }
        every { f.evaluated.evaluatedMainActions() } returns listOf(evaluatedAction(first), evaluatedAction(second))
        val other = mockk<EvaluatedIndividual<Individual>>()
        every { other.evaluatedMainActions() } returns listOf(
            evaluatedAction(RestCallResult("b", false).apply { setBody("b") }),
            evaluatedAction(RestCallResult("a", false).apply { setBody("a") }),
            evaluatedAction(RestCallResult("c", false)))
        assertFalse(f.detector.checkAndMarkConsistency(other, f.evaluated, 1))
        assertNull(first.getFlakyObservation(1))
        assertNull(second.getFlakyObservation(1))
    }

    @Test
    fun `TCP failure is a recorded execution with no retry`() {
        val f = fixture()
        val original = RestCallResult("a", false).apply { setBody("old") }
        every { f.evaluated.evaluatedMainActions() } returns listOf(
            evaluatedAction(original), evaluatedAction(RestCallResult("b", false)))
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } answers {
            secondArg<((List<ActionResult>) -> Unit)?>()!!.invoke(listOf(
                RestCallResult("a", false).apply { setBody("new") },
                RestCallResult("b", true).apply { setTcpProblem(true) }))
            null
        }
        f.detector.reexecuteToDetectFlakiness(1)
        assertNotNull(original.getFlakyObservation(1))
        assertEquals(0L, f.detector.reexecutionProgress!!.failed)
        assertTrue(f.detector.reexecutionProgress!!.complete)
        assertEquals(1L, f.detector.reexecutionProgress!!.exceptionalExecutions)
        val record = Files.readString(f.detector.observationFile!!)
        assertTrue(record.contains("TCP_PROBLEM"))
        assertTrue(record.contains("new"))
        verify(exactly = 1) { f.fitness.computeWholeAchievedCoverageForPostProcessing(any(), any()) }
    }

    private fun inject(target: Any, name: String, value: Any) {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
    }

    private class Fixture {
        val config = EMConfig().apply {
            handleFlakiness = true
            stoppingCriterion = EMConfig.StoppingCriterion.TIME
            maxTime = "1h"
        }
        val controller = ExecutionPhaseController()
        val detector = FlakinessDetector<Individual>()
        val archive = mockk<Archive<Individual>>()
        val fitness = mockk<FitnessFunction<Individual>>()
        val individual = mockk<Individual>()
        val evaluated = mockk<EvaluatedIndividual<Individual>>()
        val solution = mockk<Solution<Individual>>()
    }

    private fun fixture(): Fixture = Fixture().also { f ->
        f.config.outputFolder = output.toString()
        f.config.enableStaticFlakyInference = true
        inject(f.controller, "config", f.config)
        inject(f.detector, "config", f.config)
        inject(f.detector, "epc", f.controller)
        inject(f.detector, "archive", f.archive)
        inject(f.detector, "fitness", f.fitness)
        every { f.archive.extractSolution() } returns f.solution
        every { f.solution.individuals } returns mutableListOf(f.evaluated)
        every { f.evaluated.individual } returns f.individual
        every { f.individual.seeMainExecutableActions() } returns emptyList()
        every { f.evaluated.evaluatedMainActions() } returns emptyList()
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } returns f.evaluated
        f.controller.markStartingSearch()
    }

    private fun expire(controller: ExecutionPhaseController) {
        inject(controller, "lastPhaseStartMs", System.currentTimeMillis() - 3_600_000L)
    }

    @Test
    fun `security timeout does not skip static inference or repetitions`() {
        val f = fixture()
        f.controller.markStartingSecurity()
        expire(f.controller)
        assertTrue(f.controller.hasPhaseTimedOut(ExecutionPhaseController.Phase.SECURITY))
        f.controller.markStartingFlakiness()
        val action = mockk<EvaluatedAction>()
        val httpAction = mockk<HttpWsAction>()
        val result = RestCallResult("r1", false).apply {
            setBody("{\"createdAt\":\"2026-08-08T10:56:22.287Z\"}")
        }
        every { action.action } returns httpAction
        every { action.result } returns result
        every { f.evaluated.evaluatedMainActions() } returns listOf(action)
        f.config.execNumForDetectFlakiness = 2

        f.detector.applyPhase()

        assertNotNull(result.getStaticFlakyObservation())
        assertTrue(f.detector.staticProgress!!.complete)
        assertEquals(2L, f.detector.reexecutionProgress!!.completed)
        verify(exactly = 2) { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) }
    }

    @Test
    fun `default bounded mode respects flakiness timeout`() {
        val f = fixture()
        assertTrue(f.config.useTimeLimitForFlakiness)
        f.controller.markStartingFlakiness()
        expire(f.controller)
        f.detector.applyPhase()
        assertEquals(0L, f.detector.staticProgress!!.completed)
        assertEquals(0L, f.detector.reexecutionProgress!!.attempted)
        assertTrue(f.detector.reexecutionProgress!!.timedOut)
        assertFalse(f.detector.reexecutionProgress!!.complete)
        verify(exactly = 0) { f.fitness.computeWholeAchievedCoverageForPostProcessing(any(), any()) }
    }

    @Test
    fun `unlimited mode finishes all individuals and repetitions without changing other budgets`() {
        val f = fixture()
        f.config.useTimeLimitForFlakiness = false
        f.config.execNumForDetectFlakiness = 10
        every { f.solution.individuals } returns mutableListOf(f.evaluated, f.evaluated)
        f.controller.markStartingFlakiness()
        expire(f.controller)
        assertTrue(f.controller.hasPhaseTimedOut(ExecutionPhaseController.Phase.FLAKINESS))
        f.detector.applyPhase()
        assertEquals(2L, f.detector.staticProgress!!.completed)
        assertEquals(20L, f.detector.reexecutionProgress!!.completed)
        assertTrue(f.detector.reexecutionProgress!!.complete)
        assertEquals("1h", f.config.maxTime)
        assertEquals(0.10, f.config.extraPhaseBudgetPercentage)
        f.controller.markStartingSecurity()
        expire(f.controller)
        assertTrue(f.controller.hasPhaseTimedOut(ExecutionPhaseController.Phase.SECURITY))
    }

    @Test
    fun `budget is checked between repetitions of the same individual`() {
        val f = fixture()
        f.controller.markStartingFlakiness()
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } answers {
            expire(f.controller)
            f.evaluated
        }
        f.detector.reexecuteToDetectFlakiness(10)
        val progress = f.detector.reexecutionProgress!!
        assertEquals(1L, progress.completed)
        assertEquals(9L, progress.notAttempted)
        assertTrue(progress.timedOut)
        assertFalse(progress.complete)
    }

    @Test
    fun `null re-evaluation is not counted as completed`() {
        val f = fixture()
        f.controller.markStartingFlakiness()
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } returns null
        f.detector.reexecuteToDetectFlakiness(2)
        val progress = f.detector.reexecutionProgress!!
        assertEquals(2L, progress.attempted)
        assertEquals(2L, progress.failed)
        assertEquals(0L, progress.completed)
        assertFalse(progress.complete)
    }

    @Test
    fun `action count mismatch is recorded and counted separately from missing executions`() {
        val f = fixture()
        f.controller.markStartingFlakiness()
        val other = mockk<EvaluatedIndividual<Individual>>()
        every { other.evaluatedMainActions() } returns listOf(evaluatedAction(RestCallResult("extra", false)))
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } returns other
        f.detector.reexecuteToDetectFlakiness(1)
        assertEquals(0L, f.detector.reexecutionProgress!!.failed)
        assertEquals(1L, f.detector.reexecutionProgress!!.partialComparisons)
        assertTrue(f.detector.reexecutionProgress!!.complete)
    }

    @Test
    fun `static only does not invoke fitness and clears previous progress`() {
        val f = fixture()
        f.controller.markStartingFlakiness()
        f.detector.applyPhase()
        f.config.execNumForDetectFlakiness = 0
        f.detector.applyPhase()
        assertNull(f.detector.reexecutionProgress)
        assertTrue(f.detector.staticProgress!!.complete)
        verify(exactly = 1) { f.fitness.computeWholeAchievedCoverageForPostProcessing(any(), any()) }
    }

    @Test
    fun `empty archive completes without attempts`() {
        val f = fixture()
        f.controller.markStartingFlakiness()
        every { f.solution.individuals } returns mutableListOf()
        f.detector.applyPhase()
        assertTrue(f.detector.staticProgress!!.complete)
        assertTrue(f.detector.reexecutionProgress!!.complete)
        assertEquals(0L, f.detector.reexecutionProgress!!.expected)
    }

    @Test
    fun `evaluation exception propagates and leaves incomplete progress`() {
        val f = fixture()
        f.controller.markStartingFlakiness()
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } throws IllegalStateException("evaluation failed")
        assertThrows(IllegalStateException::class.java) { f.detector.reexecuteToDetectFlakiness(2) }
        assertEquals(1L, f.detector.reexecutionProgress!!.failed)
        assertEquals(1L, f.detector.reexecutionProgress!!.notAttempted)
        assertFalse(f.detector.reexecutionProgress!!.complete)
    }

    @Test
    fun `command line can disable only the flakiness time limit`() {
        val config = EMConfig()
        config.updateProperties(EMConfig.getOptionParser().parse(
            "--handleFlakiness", "true", "--useTimeLimitForFlakiness", "false", "--maxTime", "1h"
        ))
        assertFalse(config.useTimeLimitForFlakiness)
        assertEquals("1h", config.maxTime)
        assertEquals(0.10, config.extraPhaseBudgetPercentage)
        assertEquals(30_000, config.tcpTimeoutMs)
        assertEquals(60, config.testTimeout)
    }

    @Test
    fun `unlimited detection records actual response differences`() {
        val f = fixture()
        f.config.useTimeLimitForFlakiness = false
        f.controller.markStartingFlakiness()
        expire(f.controller)
        val httpAction = mockk<HttpWsAction>()
        val original = RestCallResult("r1", false).apply { setBody("original") }
        val changed = RestCallResult("r1", false).apply { setBody("changed") }
        val originalAction = mockk<EvaluatedAction>()
        val changedAction = mockk<EvaluatedAction>()
        every { originalAction.action } returns httpAction
        every { originalAction.result } returns original
        every { changedAction.action } returns httpAction
        every { changedAction.result } returns changed
        every { f.evaluated.evaluatedMainActions() } returns listOf(originalAction)
        val other = mockk<EvaluatedIndividual<Individual>>()
        every { other.evaluatedMainActions() } returns listOf(changedAction)
        every { f.fitness.computeWholeAchievedCoverageForPostProcessing(f.individual, any()) } returns other
        f.detector.reexecuteToDetectFlakiness(2)
        assertNotNull(original.getFlakyObservation(1))
        assertNotNull(original.getFlakyObservation(2))
        assertTrue(f.detector.reexecutionProgress!!.complete)
    }
}
