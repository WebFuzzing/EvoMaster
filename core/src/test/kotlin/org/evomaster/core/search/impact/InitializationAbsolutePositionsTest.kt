package org.evomaster.core.search.impact

import org.evomaster.core.TestUtils
import org.evomaster.core.database.mongo.MongoDbAction
import org.evomaster.core.database.redis.RedisDbAction
import org.evomaster.core.database.redis.RedisSetAction
import org.evomaster.core.database.sql.SqlAction
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class InitializationAbsolutePositionsTest {

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testMixedDatabaseMutationContextsKeepAbsolutePositions(abstract: Boolean) {
        val individual = createIndividual()
        val previous = individual.copy() as RestIndividual
        val impacts = createImpacts(individual, abstract)
        val specification = MutatedGeneSpecification()
        individual.seeInitializingActions().forEachIndexed { absoluteIndex, action ->
            if (action is SqlAction && action.representExistingData) return@forEachIndexed
            specification.addMutatedGene(
                isDb = false,
                isInit = true,
                valueBeforeMutation = "before",
                gene = action.seeTopGenes().first(),
                position = absoluteIndex,
                localId = null
            )
        }
        specification.setMutatedIndividual(individual)

        val contexts = ImpactUtils.extractMutatedGeneWithContext(specification, individual, previous, isInit = true)
        assertEquals(listOf(1, 2, 3, 4, 5), contexts.map { it.position })
        contexts.forEach { context ->
            val action = individual.seeInitializingActions()[context.position!!]
            val typeActions = individual.seeInitializingActions().filter { it.getActionGroupKey() == context.actionTypeClass }
            val geneId = ImpactUtils.generateGeneId(action, context.current)
            val expected = impacts.initActionImpacts[context.actionTypeClass]!!
                .getImpactOfAction(action.getName(), typeActions.indexOf(action))!!.geneImpacts[geneId]

            assertNotNull(expected)
            assertSame(expected, impacts.getGene(
                actionName = context.actionName,
                initActionClassName = context.actionTypeClass,
                geneId = geneId,
                actionIndex = context.position,
                localId = context.actionLocalId,
                fixedIndexedAction = true,
                fromInitialization = true,
                absoluteInitializationIndex = true
            ))
            assertEquals(context.position, context.mainPosition(context.current, context.previous, 1).position)
        }

        val sqlImpacts = impacts.initActionImpacts[ImpactsOfIndividual.SQL_ACTION_KEY]!!
        assertNull(sqlImpacts.getImpactOfAction(individual.seeInitializingActions().first().getName(), 0, absoluteIndex = true))
        val mongoImpacts = impacts.initActionImpacts[ImpactsOfIndividual.MONGODB_ACTION_KEY]!!
        assertNull(mongoImpacts.getImpactOfAction(null, 0, absoluteIndex = true))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testAbsolutePositionsRefreshAfterSqlInsertionAndRemoval(abstract: Boolean) {
        val individual = createIndividual()
        val originalIndividual = individual.copy() as RestIndividual
        val impacts = createImpacts(individual, abstract)
        val copies = listOf(impacts.copy(), impacts.clone())
        copies.forEach { assertAbsoluteLookups(originalIndividual, it) }

        val addedSql = TestUtils.generateFakeDbAction(3L, 3L, "Other")
        addedSql.doInitialize()
        individual.addInitializingDbActions(actions = listOf(addedSql))
        impacts.appendInitializationImpacts(listOf(listOf(addedSql)), ImpactsOfIndividual.SQL_ACTION_KEY, abstract)
        impacts.syncBasedOnIndividual(individual, null)

        assertEquals(3, individual.seeInitializingActions().indexOfFirst { it is MongoDbAction })
        assertEquals(5, individual.seeInitializingActions().indexOfFirst { it is RedisDbAction })
        assertAbsoluteLookups(individual, impacts)
        copies.forEach { assertAbsoluteLookups(originalIndividual, it) }

        individual.removeInitDbActions(listOf(addedSql))
        impacts.removeInitializationImpacts(listOf(addedSql to 2), 1, ImpactsOfIndividual.SQL_ACTION_KEY, abstract)
        impacts.syncBasedOnIndividual(individual, null)

        assertEquals(2, individual.seeInitializingActions().indexOfFirst { it is MongoDbAction })
        assertEquals(4, individual.seeInitializingActions().indexOfFirst { it is RedisDbAction })
        assertAbsoluteLookups(individual, impacts)

        val mongoCopy = copies.first().initActionImpacts[ImpactsOfIndividual.MONGODB_ACTION_KEY]!!
        mongoCopy.reset()
        assertNull(mongoCopy.getImpactOfAction(null, 2, absoluteIndex = true))
        assertAbsoluteLookups(originalIndividual, copies.last())
    }

    private fun assertAbsoluteLookups(individual: RestIndividual, impacts: ImpactsOfIndividual) {
        val actions = individual.seeInitializingActions()
        actions.forEachIndexed { absoluteIndex, action ->
            if (action is SqlAction && action.representExistingData) return@forEachIndexed
            val typeActions = actions.filter { it.getActionGroupKey() == action.getActionGroupKey() }
            val group = impacts.initActionImpacts[action.getActionGroupKey()]!!
            val expected = group.getImpactOfAction(action.getName(), typeActions.indexOf(action))
            assertNotNull(expected)
            assertSame(expected, group.getImpactOfAction(action.getName(), absoluteIndex, absoluteIndex = true))
        }
    }

    private fun createImpacts(individual: RestIndividual, abstract: Boolean): ImpactsOfIndividual {
        val impacts = ImpactsOfIndividual(individual, listOf(SqlAction::class, MongoDbAction::class, RedisDbAction::class), abstract, null)
        individual.seeInitializingActions().groupBy { it.getActionGroupKey() }.forEach { (type, actions) ->
            val existingData = actions.filterIsInstance<SqlAction>().count { it.representExistingData }
            val insertions = actions.filterNot { it is SqlAction && it.representExistingData }
            impacts.initInitializationImpacts(listOf(insertions), existingData, type, abstract)
        }
        impacts.syncBasedOnIndividual(individual, null)
        return impacts
    }

    private fun createIndividual(): RestIndividual {
        val sql = TestUtils.generateFakeDbAction(2L, 2L)
        val existing = SqlAction(sql.table, sql.selectedColumns, 1L,
            listOf(ImmutableDataHolderGene("Id", "1", false)), representExistingData = true)
        val mongo = listOf(
            MongoDbAction("db", "people", "ignored", listOf(StringGene("name", "first"))),
            MongoDbAction("db", "people", "ignored", listOf(StringGene("name", "second")))
        )
        val redis = listOf(
            RedisSetAction("key1", StringGene("value", "first")),
            RedisSetAction("key2", StringGene("value", "second"))
        )
        val rest = RestResourceCalls(actions = listOf(TestUtils.generateFakeQueryRestAction("1", "/foo")), sqlActions = mutableListOf())
        return RestIndividual(
            sampleType = SampleType.RANDOM,
            allActions = (listOf(existing, sql) + mongo + redis + rest).toMutableList(),
            mainSize = 1,
            sqlSize = 2,
            mongoSize = 2,
            redisSize = 2
        ).also {
            it.doInitializeLocalId()
            it.doInitialize()
        }
    }
}
