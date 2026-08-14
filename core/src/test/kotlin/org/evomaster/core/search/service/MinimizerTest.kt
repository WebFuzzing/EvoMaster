package org.evomaster.core.search.service

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.search.service.minimizer.MultiActionAction
import org.evomaster.core.search.service.minimizer.MultiActionFitness
import org.evomaster.core.search.service.minimizer.MultiActionIndividual
import org.evomaster.core.search.service.minimizer.MultiActionModule
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MinimizerTest {

    private lateinit var archive: Archive<MultiActionIndividual>
    private lateinit var fitness: MultiActionFitness
    private lateinit var config: EMConfig
    private lateinit var minimizer: Minimizer<MultiActionIndividual>

    @BeforeEach
    fun init() {

        val injector: Injector = LifecycleInjector.builder()
            .withModules(MultiActionModule(), BaseModule(arrayOf("--blackBox", "false")))
            .build().createInjector()

        archive = injector.getInstance(Key.get(
            object : TypeLiteral<Archive<MultiActionIndividual>>() {}))
        fitness = injector.getInstance(MultiActionFitness::class.java)
        config = injector.getInstance(EMConfig::class.java)
        minimizer = injector.getInstance(Key.get(
            object : TypeLiteral<Minimizer<MultiActionIndividual>>() {}))

        config.seed = 42
        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
    }

    @Test
    fun testCoverageIsNotLostWhenMinimizingMultiActionTarget() {

        val ind = MultiActionIndividual(listOf(
            MultiActionAction(0),
            MultiActionAction(1),
            MultiActionAction(2)
        ))
        TestUtils.doInitializeIndividualForTesting(ind)

        fitness.targetRequirements = mapOf(
            100 to setOf(0),
            101 to setOf(2),
            200 to setOf(0, 1)  //only covered when actions 0 AND 1 are both present
        )

        val evaluated = fitness.computeWholeAchievedCoverageForPostProcessing(ind)!!
        val added = archive.addIfNeeded(evaluated)
        assertTrue(added)

        assertTrue(archive.isCovered(100))
        assertTrue(archive.isCovered(101))
        assertTrue(archive.isCovered(200))

        minimizer.applyPhase()

        assertTrue(archive.isCovered(100), "Lost target 100, which only needs 1 action")
        assertTrue(archive.isCovered(101), "Lost target 101, which only needs 1 action")
        assertTrue(archive.isCovered(200), "Lost target 200, which needs actions 0 and 1 together")
    }

    @Test
    fun testCoverageIsNotLostDueToNonDeterministicReEvaluation() {

        val ind = MultiActionIndividual(listOf(
            MultiActionAction(0),
            MultiActionAction(1)
        ))
        TestUtils.doInitializeIndividualForTesting(ind)

        fitness.targetRequirements = mapOf(
            100 to setOf(0),
            300 to setOf(0)
        )
        fitness.flakyTarget = 300

        val evaluated = fitness.computeWholeAchievedCoverageForPostProcessing(ind)!!
        val added = archive.addIfNeeded(evaluated)
        assertTrue(added)

        assertTrue(archive.isCovered(100))
        assertTrue(archive.isCovered(300))

        minimizer.applyPhase()

        assertTrue(archive.isCovered(100), "Lost target 100, which is deterministic")
        assertTrue(archive.isCovered(300), "Lost target 300 due to non-deterministic re-evaluation during minimization")
    }
}
