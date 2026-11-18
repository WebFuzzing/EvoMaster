package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.TestUtils
import org.evomaster.core.database.mongo.MongoDbAction
import org.evomaster.core.database.sql.SqlAction
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.CustomMutationRateGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.TupleGeneImpact
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImpactUtilsTest {

    @Test
    fun testCreateGeneImpactForImmutableDataHolderAndComposites() {
        val holder = ImmutableDataHolderGene("raw", "value", inQuotes = true)
        val holderImpact = ImpactUtils.createGeneImpact(holder, "rawId")
        assertEquals(GeneImpact::class.java, holderImpact::class.java)

        val obj = ObjectGene("obj", listOf(StringGene("s", "a")))
        assertTrue(ImpactUtils.createGeneImpact(obj, "objId") is ObjectGeneImpact)

        val tuple = TupleGene("tup", listOf(StringGene("s", "a")))
        assertTrue(ImpactUtils.createGeneImpact(tuple, "tupId") is TupleGeneImpact)
    }

    @Test
    fun testGenerateGeneIdForWrappersObjectsAndIndividualActions() {
        val objWithRef = ObjectGene("payload", listOf(StringGene("s", "a")), refType = "MyDto")
        val objNoRef = ObjectGene("payload", listOf(StringGene("s", "a")))
        val wrapped = CustomMutationRateGene(
            "wrapper",
            OptionalGene("opt", objWithRef),
            probability = 0.5
        )

        assertEquals("MyDto>payload", ImpactUtils.generateGeneId(objWithRef))
        assertEquals("payload", ImpactUtils.generateGeneId(objNoRef))
        assertEquals("DisruptiveGene>OptionalGene>MyDto>payload", ImpactUtils.generateGeneId(wrapped))

        val initAction = MongoDbAction("db", "items", "type", listOf(StringGene("itemId", "v1")))
        val restAction = TestUtils.generateFakeQueryRestAction("GET:/items", "/items")
        val individual = RestIndividual(
            sampleType = SampleType.RANDOM,
            allActions = mutableListOf(initAction, RestResourceCalls(actions = listOf(restAction), sqlActions = mutableListOf())),
            mainSize = 1,
            mongoSize = 1
        ).also { TestUtils.doInitializeIndividualForTesting(it) }

        val initGene = initAction.seeTopGenes().first()
        val mainGene = restAction.seeTopGenes().last()
        val detachedGene = StringGene("detached", "x")

        val initId = ImpactUtils.generateGeneId(individual, initGene)
        assertEquals("${initAction.getName()}::itemId::0", initId)
        assertEquals(initAction.getName(), ImpactUtils.extractActionName(initId))

        val mainId = ImpactUtils.generateGeneId(individual, mainGene)
        assertEquals("${restAction.getName()}::${mainGene.name}::1", mainId)
        assertEquals(restAction.getName(), ImpactUtils.extractActionName(mainId))

        assertEquals("detached", ImpactUtils.generateGeneId(individual, detachedGene))
        assertNull(ImpactUtils.extractActionName("detached"))
    }

    @Test
    fun testExtractMutatedGeneWithContextForInitAndMainActions() {
        val existingTemplate = TestUtils.generateFakeDbAction(1L, 1L, "Foo")
        val existingSql = SqlAction(
            existingTemplate.table,
            existingTemplate.selectedColumns,
            1L,
            listOf(ImmutableDataHolderGene("Id", "1", false)),
            representExistingData = true
        )
        val insertedSql = TestUtils.generateFakeDbAction(2L, 2L, "Foo", intValue = 10)
        val restAction = TestUtils.generateFakeQueryRestAction("GET:/foo", "/foo")

        val individual = RestIndividual(
            sampleType = SampleType.RANDOM,
            allActions = mutableListOf(
                existingSql,
                insertedSql,
                RestResourceCalls(actions = listOf(restAction), sqlActions = mutableListOf())
            ),
            mainSize = 1,
            sqlSize = 2
        ).also { TestUtils.doInitializeIndividualForTesting(it) }

        val previous = individual.copy() as RestIndividual
        val mutatedInitGene = insertedSql.seeTopGenes().first()
        val mutatedMainGene = restAction.seeTopGenes().first() as IntegerGene
        mutatedMainGene.value += 1

        val spec = MutatedGeneSpecification()
        spec.addMutatedGene(
            isDb = false,
            isInit = true,
            valueBeforeMutation = "10",
            gene = mutatedInitGene,
            position = 1,
            localId = insertedSql.getLocalId()
        )
        spec.addMutatedGene(
            isDb = false,
            isInit = false,
            valueBeforeMutation = "${mutatedMainGene.value - 1}",
            gene = mutatedMainGene,
            position = 0,
            localId = restAction.getLocalId()
        )
        spec.setMutatedIndividual(individual)

        val initContexts = ImpactUtils.extractMutatedGeneWithContext(spec, individual, previous, isInit = true)
        assertEquals(1, initContexts.size)
        assertSame(mutatedInitGene, initContexts.single().current)
        assertNotNull(initContexts.single().previous)
        assertEquals(insertedSql.getName(), initContexts.single().actionName)
        assertEquals(1, initContexts.single().position)
        assertFalse(initContexts.single().isDynamicAction)
        assertEquals(2, initContexts.single().numOfMutatedGene)

        val mainContexts = ImpactUtils.extractMutatedGeneWithContext(spec, individual, previous, isInit = false)
        assertEquals(1, mainContexts.size)
        assertSame(mutatedMainGene, mainContexts.single().current)
        assertEquals(mutatedMainGene.value - 1, (mainContexts.single().previous as IntegerGene).value)
        assertEquals(restAction.getName(), mainContexts.single().actionName)
        assertEquals(0, mainContexts.single().position)
        assertEquals(2, mainContexts.single().numOfMutatedGene)
    }

    @Test
    fun testFindMutatedGeneHandlesHomogeneousAndHeterogeneousComposites() {
        val templateObj = ObjectGene("doc", listOf(StringGene("k", "old")))
        val changedObj = ObjectGene("doc", listOf(StringGene("k", "new")))
        val sameObj = ObjectGene("doc", listOf(StringGene("k", "old")))
        assertSame(changedObj, ImpactUtils.findMutatedGene(listOf(sameObj, changedObj), templateObj))
        assertNull(ImpactUtils.findMutatedGene(listOf(sameObj), templateObj, includeSameValue = false))
        assertSame(sameObj, ImpactUtils.findMutatedGene(listOf(sameObj), templateObj, includeSameValue = true))

        val wrappedChanged = OptionalGene("doc", ObjectGene("doc", listOf(StringGene("k", "wrappedNew"))))
        assertSame(wrappedChanged, ImpactUtils.findMutatedGene(listOf(wrappedChanged), templateObj))

        val heteroObj = ObjectGene("doc", listOf(IntegerGene("k", 1)))
        val heteroTuple = TupleGene("arr", listOf(IntegerGene("elem", 1)))
        val templateTuple = TupleGene("arr", listOf(StringGene("elem", "x")))

        assertNotNull(ImpactUtils.findMutatedGene(listOf(heteroObj), templateObj))
        assertNotNull(ImpactUtils.findMutatedGene(listOf(heteroTuple), templateTuple))
    }

    @Test
    fun testPrintGeneToRootActionFormatsHierarchyWithAndWithoutValue() {
        val leaf = StringGene("field", "hello")
        val doc = ObjectGene("doc", listOf(leaf))
        MongoDbAction("db", "items", "type", listOf(doc))

        val nl = System.lineSeparator()
        val withValue = ImpactUtils.printGeneToRootAction(leaf, doIncludeGeneValue = true)
        assertEquals(
            "${nl}GeneValue:hello${nl}|-MongoDbAction[MONGO_Insert_db_items_type]${nl} |-ObjectGene::doc${nl}  |-StringGene::field",
            withValue
        )

        val withoutValue = ImpactUtils.printGeneToRootAction(leaf, doIncludeGeneValue = false)
        assertEquals(
            "${nl}|-MongoDbAction[MONGO_Insert_db_items_type]${nl} |-ObjectGene::doc${nl}  |-StringGene::field",
            withoutValue
        )
    }
}
