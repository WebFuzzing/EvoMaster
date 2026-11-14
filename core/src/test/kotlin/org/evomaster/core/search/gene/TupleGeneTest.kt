package org.evomaster.core.search.gene

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.algorithms.constant.ConstantIndividual
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.impact.impactinfocollection.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.impactinfocollection.MutatedGeneWithContext
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.StringGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.TupleGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.ArchiveImpactSelector
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TupleGeneTest {


    @Test
    fun copyFromAndcontainsSameValueAsTest() {

        val originalGene = TupleGene(
            "tupleGeneTestOri",
            mutableListOf(
                StringGene("stringGene1", "hello"),
                ObjectGene("objectGene2", listOf(IntegerGene("f1", 3))),
                ArrayGene(
                    "arrayGene3",
                    ObjectGene("ObjectGene1", listOf(FloatGene("f1", 3.43f)))
                )
            )
        )

        val gene = TupleGene(
            "tupleGeneTest",
            mutableListOf(
                StringGene("stringGene1", "foo"),
                ObjectGene("objectGene2", listOf(IntegerGene("f1", 42))),
                ArrayGene(
                    "arrayGene3",
                    ObjectGene("ObjectGene1", listOf(FloatGene("f1", 4.2f)))
                ).apply {
                    addElement(ObjectGene("ObjectGene1", listOf(FloatGene("f1", 4.2f))))
                }
            )
        )

        assertFalse(originalGene.containsSameValueAs(gene))

        originalGene.unsafeCopyValueFrom(gene)
        assertEquals(1, (originalGene.elements[2] as ArrayGene<*>).getViewOfElements().size)
        assertTrue(originalGene.containsSameValueAs(gene))
    }

    @Test
    fun bindValueBasedOn() {
        val originalGene = TupleGene(
            "tupleGeneTestOri",
            mutableListOf(
                StringGene("stringGene1", "hello"),
                ObjectGene("objectGene2", listOf(IntegerGene("f1", 3))),
                ArrayGene(
                    "arrayGene3",
                    ObjectGene("ObjectGene1", listOf(FloatGene("f1", 3.43f)))
                )
            )
        )

        val gene = TupleGene(
            "tupleGeneTest",
            mutableListOf(
                StringGene("stringGene1", "foo"),
                ObjectGene("objectGene2", listOf(IntegerGene("f1", 42))),
                ArrayGene(
                    "arrayGene3",
                    ObjectGene("ObjectGene1", listOf(FloatGene("f1", 4.2f)))
                ).apply {
                    addElement(ObjectGene("ObjectGene1", listOf(FloatGene("f1", 4.2f))))
                }
            )
        )

        assertTrue(originalGene.unsafeCopyValueFrom(gene))

        originalGene.elements.apply {
            assertTrue(this[0] is StringGene)
            assertEquals("foo", (this[0] as StringGene).value)
            assertTrue(this[1] is ObjectGene)
            assertTrue((this[1] as ObjectGene).fields[0] is IntegerGene)
            assertEquals(42, ((this[1] as ObjectGene).fields[0] as IntegerGene).value)
            assertTrue(this[2] is ArrayGene<*>)
            assertEquals(1, (this[2] as ArrayGene<*>).getViewOfElements().size)
        }
    }

    @Test
    fun lastElement(){



    }

    @Test
    fun impactAndAdaptiveMutationWithDistinctElementNamesTest() {
        val services = createMutationServices()
        val original = TupleGene(
            "tupleWithDistinctNames",
            listOf(
                StringGene("elemStr", "hello"),
                BigDecimalGene("elemNum", BigDecimal("10")),
                ObjectGene("elemObj", listOf(IntegerGene("nestedInt", 5)))
            )
        ).apply { doInitialize(services.randomness) }

        val impact = TupleGeneImpact("tupleId", original)
        assertTrue(impact.elements["elemStr"] is StringGeneImpact)
        assertTrue(impact.elements["elemObj"] is ObjectGeneImpact)

        // Initial (previous == null) impact recording
        impact.countImpactWithMutatedGeneWithContext(
            MutatedGeneWithContext(current = original, previous = null),
            noImpactTargets = setOf(),
            impactTargets = setOf(),
            improvedTargets = setOf(),
            onlyManipulation = false
        )
        assertEquals(1, impact.getTimesToManipulate())
        assertEquals(1, impact.elements.getValue("elemStr").getTimesToManipulate())
        assertEquals(1, impact.elements.getValue("elemNum").getTimesToManipulate())
        assertEquals(1, impact.elements.getValue("elemObj").getTimesToManipulate())

        // Subsequent mutation on a specific element updates only that element's impact
        val mutatedStr = original.copy() as TupleGene
        val strChild = mutatedStr.elements[0] as StringGene
        strChild.value = "${strChild.value}_updated"

        impact.countImpactWithMutatedGeneWithContext(
            MutatedGeneWithContext(current = mutatedStr, previous = original),
            noImpactTargets = setOf(),
            impactTargets = setOf(1),
            improvedTargets = setOf(1),
            onlyManipulation = false
        )
        assertEquals(1.0, impact.elements.getValue("elemStr").getTimesOfImpacts()[1])
        assertNull(impact.elements.getValue("elemNum").getTimesOfImpacts()[1])
        assertNull(impact.elements.getValue("elemObj").getTimesOfImpacts()[1])

        // Adaptive mutation works with matching element impacts
        val mutatedIndices = mutableSetOf<Int>()
        repeat(40) { i ->
            val candidate = original.copy() as TupleGene
            val info = services.createMutationInfo(candidate, impact, setOf(1))
            candidate.standardMutation(
                randomness = services.randomness,
                apc = services.apc,
                mwc = services.mwc,
                childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT,
                enableAdaptiveGeneValueMutation = (i % 2 == 1),
                additionalGeneMutationInfo = info
            )
            candidate.elements.indices.forEach { idx ->
                if (!candidate.elements[idx].containsSameValueAs(original.elements[idx])) {
                    mutatedIndices.add(idx)
                }
            }
        }
        assertEquals(setOf(0, 1, 2), mutatedIndices)
    }

    @Test
    fun tupleGeneImpactWithHeterogeneousSameNamedElementsTest() {
        val randomness = Randomness().apply { updateSeed(42) }
        val original = TupleGene(
            "items",
            listOf(
                StringGene("items", "alpha"),
                ObjectGene("items", listOf(IntegerGene("nested", 1))),
                BigDecimalGene("items", BigDecimal("10")),
                BooleanGene("items", true)
            )
        ).apply { doInitialize(randomness) }

        val impact = TupleGeneImpact("itemsId", original)
        // Map keyed by gene.name retains only the last element's impact type
        assertTrue(impact.elements.getValue("items") is BinaryGeneImpact)

        // Initial recording must skip mismatched elements instead of throwing
        impact.countImpactWithMutatedGeneWithContext(
            MutatedGeneWithContext(current = original, previous = null),
            noImpactTargets = setOf(),
            impactTargets = setOf(),
            improvedTargets = setOf(),
            onlyManipulation = false
        )
        assertEquals(1, impact.getTimesToManipulate())
        assertEquals(1, impact.elements.getValue("items").getTimesToManipulate())

        // Mutating a mismatched element (StringGene) does not throw
        val mutatedString = original.copy() as TupleGene
        (mutatedString.elements[0] as StringGene).value = "beta"
        impact.countImpactWithMutatedGeneWithContext(
            MutatedGeneWithContext(current = mutatedString, previous = original),
            noImpactTargets = setOf(),
            impactTargets = setOf(1),
            improvedTargets = setOf(1),
            onlyManipulation = false
        )
        assertEquals(2, impact.getTimesToManipulate())

        // Mutating a mismatched composite element (ObjectGene) does not throw
        val mutatedObject = original.copy() as TupleGene
        ((mutatedObject.elements[1] as ObjectGene).fields[0] as IntegerGene).value += 1
        impact.countImpactWithMutatedGeneWithContext(
            MutatedGeneWithContext(current = mutatedObject, previous = original),
            noImpactTargets = setOf(),
            impactTargets = setOf(2),
            improvedTargets = setOf(2),
            onlyManipulation = false
        )
        assertEquals(3, impact.getTimesToManipulate())

        // Mutating the matching-type element (BooleanGene) still records impact
        val mutatedBool = original.copy() as TupleGene
        (mutatedBool.elements[3] as BooleanGene).value = !(mutatedBool.elements[3] as BooleanGene).value
        impact.countImpactWithMutatedGeneWithContext(
            MutatedGeneWithContext(current = mutatedBool, previous = original),
            noImpactTargets = setOf(),
            impactTargets = setOf(3),
            improvedTargets = setOf(3),
            onlyManipulation = false
        )
        assertEquals(1.0, impact.elements.getValue("items").getTimesOfImpacts()[3])
    }

    @Test
    fun adaptiveMutationWithHeterogeneousSameNamedElementsTest() {
        val services = createMutationServices()
        // Last element is BooleanGene so StringGene, ObjectGene, and BigDecimalGene receive impact = null
        val original = TupleGene(
            "items",
            listOf(
                StringGene("items", "alpha"),
                ObjectGene("items", listOf(IntegerGene("nested", 1), StringGene("label", "ok"))),
                BigDecimalGene("items", BigDecimal("10")),
                BooleanGene("items", true)
            )
        ).apply { doInitialize(services.randomness) }

        val impact = TupleGeneImpact("itemsId", original)
        val mutatedIndices = mutableSetOf<Int>()

        // Run enough mutations to exercise all child types with both adaptive value mutation modes
        repeat(60) { i ->
            val candidate = original.copy() as TupleGene
            val info = services.createMutationInfo(candidate, impact, setOf(1))
            candidate.standardMutation(
                randomness = services.randomness,
                apc = services.apc,
                mwc = services.mwc,
                childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT,
                enableAdaptiveGeneValueMutation = (i % 2 == 1),
                additionalGeneMutationInfo = info
            )
            candidate.elements.indices.forEach { idx ->
                if (!candidate.elements[idx].containsSameValueAs(original.elements[idx])) {
                    mutatedIndices.add(idx)
                }
            }
        }

        // Verify mismatched leaf (0, 2), mismatched composite (1), and matching leaf (3) were all safely mutated
        assertEquals(setOf(0, 1, 2, 3), mutatedIndices)
    }

    private data class MutationServices(
        val randomness: Randomness,
        val apc: AdaptiveParameterControl,
        val mwc: MutationWeightControl,
        val ags: ArchiveImpactSelector,
        val agm: ArchiveGeneMutator,
        val dummyEvi: EvaluatedIndividual<ConstantIndividual>
    ) {
        fun createMutationInfo(rootGene: Gene, impact: TupleGeneImpact, targets: Set<Int>): AdditionalGeneMutationInfo {
            return AdditionalGeneMutationInfo(
                selection = GeneMutationSelectionMethod.APPROACH_IMPACT,
                impact = impact,
                geneReference = rootGene.name,
                archiveGeneSelector = ags,
                archiveGeneMutator = agm,
                evi = dummyEvi,
                targets = targets,
                localId = null,
                rootGene = rootGene
            )
        }
    }

    private fun createMutationServices(): MutationServices {
        val injector: Injector = LifecycleInjector.builder()
            .withModules(*arrayOf<Module>(BaseModule(arrayOf("--blackBox", "false"), true)))
            .build().createInjector()
        val config = injector.getInstance(EMConfig::class.java).apply {
            stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            maxEvaluations = 100
            seed = 42
            weightBasedMutationRate = true
        }
        val randomness = injector.getInstance(Randomness::class.java).apply { updateSeed(config.seed) }
        val apc = injector.getInstance(AdaptiveParameterControl::class.java)
        val mwc = injector.getInstance(MutationWeightControl::class.java)
        val ags = injector.getInstance(ArchiveImpactSelector::class.java)
        val agm = injector.getInstance(ArchiveGeneMutator::class.java)
        val dummyInd = ConstantIndividual(IntegerGene("dummy", 0)).also {
            TestUtils.doInitializeIndividualForTesting(it, randomness)
        }
        val dummyEvi = EvaluatedIndividual(FitnessValue(1.0), dummyInd, listOf())
        return MutationServices(randomness, apc, mwc, ags, agm, dummyEvi)
    }
}
