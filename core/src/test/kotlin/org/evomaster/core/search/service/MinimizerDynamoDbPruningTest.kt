package org.evomaster.core.search.service

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.database.dynamodb.DynamoDbAction
import org.evomaster.core.database.dynamodb.DynamoDbAttributeGene
import org.evomaster.core.database.dynamodb.DynamoDbValueType
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinimizerDynamoDbPruningTest {

    private class StubRestFitness : FitnessFunction<RestIndividual>() {
        override fun doCalculateCoverage(
            individual: RestIndividual,
            targets: Set<Int>,
            allTargets: Boolean,
            fullyCovered: Boolean,
            descriptiveIds: Boolean
        ): EvaluatedIndividual<RestIndividual>? {
            val requiredPresent = individual.seeDynamoDbActions().any { it.tableName == "RequiredTable" }
            val fv = FitnessValue(individual.size().toDouble())
            fv.updateTarget(1, if (requiredPresent) 1.0 else 0.0)
            val results = individual.seeAllActions().map { ActionResult(it.getLocalId()) }
            return EvaluatedIndividual(fv, individual.copy() as RestIndividual, results)
        }
    }

    private fun createInjector(): Injector = LifecycleInjector.builder()
        .withModules(
            BaseModule(arrayOf("--blackBox", "false"), true),
            object : AbstractModule() {
                override fun configure() {
                    bind(object : TypeLiteral<Archive<RestIndividual>>() {}).asEagerSingleton()
                    bind(object : TypeLiteral<Archive<*>>() {})
                        .to(Key.get(object : TypeLiteral<Archive<RestIndividual>>() {}))
                    bind(Archive::class.java)
                        .to(Key.get(object : TypeLiteral<Archive<RestIndividual>>() {}))
                    bind(object : TypeLiteral<FitnessFunction<RestIndividual>>() {})
                        .to(StubRestFitness::class.java)
                        .asEagerSingleton()
                    bind(object : TypeLiteral<Minimizer<RestIndividual>>() {}).asEagerSingleton()
                }
            }
        )
        .build().createInjector()

    @Test
    fun testPrunesRedundantDynamoDbActionsWhileKeepingRequiredOnes() {
        val injector = createInjector()

        val config = injector.getInstance(EMConfig::class.java)
        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
        val archive = injector.getInstance(Key.get(object : TypeLiteral<Archive<RestIndividual>>() {}))
        val fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<RestIndividual>>() {}))
        val minimizer = injector.getInstance(Key.get(object : TypeLiteral<Minimizer<RestIndividual>>() {}))

        val requiredDdb = DynamoDbAction(
            "RequiredTable",
            listOf(DynamoDbAttributeGene("id", DynamoDbValueType.STRING, StringGene("id", "req")))
        )
        val redundantDdb = DynamoDbAction(
            "UnusedTable",
            listOf(DynamoDbAttributeGene("id", DynamoDbValueType.STRING, StringGene("id", "unused")))
        )
        val restCall = RestResourceCalls(
            actions = listOf(TestUtils.generateFakeQueryRestAction("1", "/items")),
            sqlActions = mutableListOf()
        )
        val ind = RestIndividual(
            sampleType = SampleType.RANDOM,
            allActions = mutableListOf(requiredDdb, redundantDdb, restCall),
            mainSize = 1,
            dynamoDbSize = 2
        )
        TestUtils.doInitializeIndividualForTesting(ind)

        val evaluated = fitness.calculateCoverage(ind, modifiedSpec = null)!!
        archive.addIfNeeded(evaluated)

        minimizer.applyPhase()

        val minimized = archive.extractSolution().individuals.single().individual
        val remainingDdb = minimized.seeActions(ActionFilter.ONLY_DYNAMODB).map { (it as DynamoDbAction).tableName }
        assertEquals(listOf("RequiredTable"), remainingDdb)
    }

    @Test
    fun testPreservesIndividualWithoutRedundantDynamoDbActions() {
        val injector = createInjector()

        val config = injector.getInstance(EMConfig::class.java)
        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
        val archive = injector.getInstance(Key.get(object : TypeLiteral<Archive<RestIndividual>>() {}))
        val fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<RestIndividual>>() {}))
        val minimizer = injector.getInstance(Key.get(object : TypeLiteral<Minimizer<RestIndividual>>() {}))

        val requiredDdb = DynamoDbAction(
            "RequiredTable",
            listOf(DynamoDbAttributeGene("id", DynamoDbValueType.STRING, StringGene("id", "req")))
        )
        val restCall = RestResourceCalls(
            actions = listOf(TestUtils.generateFakeQueryRestAction("1", "/items")),
            sqlActions = mutableListOf()
        )
        val ind = RestIndividual(
            sampleType = SampleType.RANDOM,
            allActions = mutableListOf(requiredDdb, restCall),
            mainSize = 1,
            dynamoDbSize = 1
        )
        TestUtils.doInitializeIndividualForTesting(ind)

        val evaluated = fitness.calculateCoverage(ind, modifiedSpec = null)!!
        archive.addIfNeeded(evaluated)

        minimizer.applyPhase()

        val minimized = archive.extractSolution().individuals.single().individual
        val remainingDdb = minimized.seeActions(ActionFilter.ONLY_DYNAMODB).map { (it as DynamoDbAction).tableName }
        assertEquals(listOf("RequiredTable"), remainingDdb)
    }
}
