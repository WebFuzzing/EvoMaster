package org.evomaster.core.search.service

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.TestUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.action.MainAction
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.search.tracer.TrackOperator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.min

class MinimizerTest {
    private fun getInjector(): Injector {
        val args = arrayOf(
            "--blackBox=false",
            "--seed=42",
            "--createTests=false",
            "--stoppingCriterion=ACTION_EVALUATIONS",
            "--enableAdvancedMinimization=true",
        )

        val injector: Injector = LifecycleInjector.builder()
            .withModules(TestModule(), BaseModule(args))
            .build().createInjector()
        return injector
    }


    class TestMainAction(val list: MutableList<EnumGene<Double>> = mutableListOf()) : MainAction(false, list) {
        override fun getName(): String {
            return "Test Action"
        }

        override fun seeTopGenes(): List<Gene> {
            return list
        }

        override fun copyContent(): StructuralElement {
            return TestMainAction(list.map { it.copy() as EnumGene<Double> }.toMutableList())
        }
    }


    class TestIndividual(
        var numberOfMainActions: Int,
        val numberOfGenesPerAction: Int,
        trackOperator: TrackOperator? = null,
        index: Int = -1,
        mainActions: MutableList<TestMainAction> = createMainActions(numberOfMainActions, numberOfGenesPerAction)
    ) : Individual(trackOperator, index, mainActions, { k -> TestMainAction::class.java.isAssignableFrom(k) }) {

        companion object {
            fun createMainActions(numberOfMainActions: Int, numberOfGenesPerAction: Int): MutableList<TestMainAction> {
                val mainActions = mutableListOf<TestMainAction>()
                repeat(numberOfMainActions) {
                    val genes = mutableListOf<EnumGene<Double>>()
                    repeat(numberOfGenesPerAction) {
                        val gene = EnumGene<Double>("$it", listOf(0.0, 0.25, 0.5, 0.75, 1.0), 0)
                        genes.add(gene)
                    }
                    mainActions.add(TestMainAction(genes))
                }
                return mainActions
            }
        }

        private fun getAction(index: Int) = children[index] as TestMainAction

        fun getValue(actionIndex: Int, geneIndex: Int): Double {
            val gene = getAction(actionIndex).list[geneIndex]
            return gene.values[gene.index]
        }

        fun setValue(actionIndex: Int, geneIndex: Int, value: Double) {
            val gene = getAction(actionIndex).list[geneIndex]
            when (value) {
                0.0  -> gene.index = 0
                0.25 -> gene.index = 1
                0.5  -> gene.index = 2
                0.75 -> gene.index = 3
                1.0  -> gene.index = 4
                else -> throw IllegalArgumentException("Invalid value $value")
            }
        }

        override fun copyContent(): Individual {
            return TestIndividual(
                numberOfMainActions,
                numberOfGenesPerAction,
                trackOperator,
                index,
                children.toMutableList() as MutableList<TestMainAction>
            )
        }

        override fun seeTopGenes(filter: ActionFilter): List<Gene> {
            val topGenes = mutableListOf<EnumGene<Double>>()
            children.forEach { mainAction ->
                topGenes.addAll((mainAction as TestMainAction).list)
            }
            return topGenes
        }

        override fun size(): Int {
            return children.size
        }

        override fun seeMainExecutableActions(): List<MainAction> {
            return children.filterIsInstance<TestMainAction>()
        }

        override fun removeMainExecutableAction(relativeIndex: Int) {
            super.removeMainExecutableAction(relativeIndex)
            numberOfMainActions--
        }

        override fun isValidInitializationActions(errors: MutableList<String>?): Boolean {
            return true
        }

        override fun repairInitializationActions(randomness: Randomness) {

        }
    }


    class TestFitness : FitnessFunction<TestIndividual>() {
        override fun doCalculateCoverage(
            individual: TestIndividual,
            targets: Set<Int>,
            allTargets: Boolean,
            fullyCovered: Boolean,
            descriptiveIds: Boolean,
        ): EvaluatedIndividual<TestIndividual>? {
            val fv = FitnessValue(individual.size().toDouble())

            (0 until individual.numberOfGenesPerAction).forEach { targetIndex ->
                val targetScore = (0 until individual.numberOfMainActions).sumOf { actionIndex ->
                    individual.getValue(actionIndex, targetIndex)
                }
                fv.updateTarget(targetIndex, min(targetScore, FitnessValue.MAX_VALUE))
            }

            return EvaluatedIndividual(
                fv,
                individual.copy() as TestIndividual,
                listOf(),
                config = config,
                trackOperator = individual.trackOperator,
                index = if (config.trackingEnabled()) time.evaluatedIndividuals else Traceable.DEFAULT_INDEX
            )
        }
    }


    class TestModule : AbstractModule() {

        override fun configure() {
            bind(object : TypeLiteral<FitnessFunction<TestIndividual>>() {})
                .to(TestFitness::class.java)
                .asEagerSingleton()

            bind(object : TypeLiteral<Archive<TestIndividual>>() {})
                .asEagerSingleton()

            bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<TestIndividual>>() {})
                .asEagerSingleton()
        }
    }

    @Test
    fun testMinimizerReducesIndividualToASingleAction() {
        val injector = getInjector()
        val archive = injector.getInstance(Key.get(object : TypeLiteral<Archive<TestIndividual>>() {}))
        val ff = injector.getInstance(TestFitness::class.java)

        val ind = TestIndividual(3, 1)
        TestUtils.doInitializeIndividualForTesting(ind)
        ind.setValue(1, 0, 1.0)

        ff.calculateCoverage(ind)?.run {
            archive.addIfNeeded(this)
        }

        val coveredTargetsBeforeMinimization = archive.coveredTargets()
        assertEquals(1, coveredTargetsBeforeMinimization.size)

        val minimizer = injector.getInstance(Key.get(object : TypeLiteral<Minimizer<TestIndividual>>() {}))
        minimizer.applyPhase()

        val coveredTargetsAfterMinimization = archive.coveredTargets()
        assert(coveredTargetsAfterMinimization == coveredTargetsBeforeMinimization)

        assertEquals(1, archive.extractSolution().individuals.first().individual.size())
    }

    @Test
    fun testMinimizerReducesIndividualToMultipleActions() {
        val injector = getInjector()
        val archive = injector.getInstance(Key.get(object : TypeLiteral<Archive<TestIndividual>>() {}))
        val ff = injector.getInstance(TestFitness::class.java)

        val ind = TestIndividual(3, 2)
        TestUtils.doInitializeIndividualForTesting(ind)
        ind.setValue(0, 0, 0.5)
        ind.setValue(2, 0, 0.5)

        ff.calculateCoverage(ind)?.run {
            archive.addIfNeeded(this)
        }

        val coveredTargetsBeforeMinimization = archive.coveredTargets()
        assertEquals(1, coveredTargetsBeforeMinimization.size)

        val minimizer = injector.getInstance(Key.get(object : TypeLiteral<Minimizer<TestIndividual>>() {}))
        minimizer.applyPhase()

        val coveredTargetsAfterMinimization = archive.coveredTargets()
        assert(coveredTargetsAfterMinimization == coveredTargetsBeforeMinimization)

        assertEquals(2, archive.extractSolution().individuals.first().individual.size())
    }

    @Test
    fun testMinimizesToDifferentSingleActionIndividuals() {
        val injector = getInjector()
        val archive = injector.getInstance(Key.get(object : TypeLiteral<Archive<TestIndividual>>() {}))
        val ff = injector.getInstance(TestFitness::class.java)

        val ind = TestIndividual(3, 2)
        TestUtils.doInitializeIndividualForTesting(ind)
        ind.setValue(0, 0, 1.0)
        ind.setValue(1, 1, 1.0)

        ff.calculateCoverage(ind)?.run {
            archive.addIfNeeded(this)
        }

        assertEquals(1, archive.extractSolution().individuals.size)

        val coveredTargetsBeforeMinimization = archive.coveredTargets()
        assertEquals(2, coveredTargetsBeforeMinimization.size)

        val minimizer = injector.getInstance(Key.get(object : TypeLiteral<Minimizer<TestIndividual>>() {}))
        minimizer.applyPhase()

        val coveredTargetsAfterMinimization = archive.coveredTargets()
        assert(coveredTargetsAfterMinimization == coveredTargetsBeforeMinimization)

        assertEquals(2, archive.extractSolution().individuals.size)
        assert(archive.extractSolution().individuals.all { it.individual.size() == 1 })
    }

    @Test
    fun testMinimizerCreatesDifferentIndividualForOneTarget() {
        val injector = getInjector()
        val archive = injector.getInstance(Key.get(object : TypeLiteral<Archive<TestIndividual>>() {}))
        val ff = injector.getInstance(TestFitness::class.java)

        val ind = TestIndividual(3, 2)
        TestUtils.doInitializeIndividualForTesting(ind)

        // The first target only needs two actions to be covered
        ind.setValue(0, 0, 0.5)
        ind.setValue(2, 0, 0.5)

        // The second target needs all actions to be covered
        ind.setValue(0, 1, 0.25)
        ind.setValue(1, 1, 0.25)
        ind.setValue(2, 1, 0.5)

        ff.calculateCoverage(ind)?.run {
            archive.addIfNeeded(this)
        }

        assertEquals(1, archive.extractSolution().individuals.size)

        val coveredTargetsBeforeMinimization = archive.coveredTargets()
        assertEquals(2, coveredTargetsBeforeMinimization.size)

        val minimizer = injector.getInstance(Key.get(object : TypeLiteral<Minimizer<TestIndividual>>() {}))
        minimizer.applyPhase()

        val coveredTargetsAfterMinimization = archive.coveredTargets()
        assert(coveredTargetsAfterMinimization == coveredTargetsBeforeMinimization)

        assertEquals(2, archive.extractSolution().individuals.size)
        assert(archive.extractSolution().individuals.any { it.individual.size() == 2 })
        assert(archive.extractSolution().individuals.any { it.individual.size() == 3 })
    }
}