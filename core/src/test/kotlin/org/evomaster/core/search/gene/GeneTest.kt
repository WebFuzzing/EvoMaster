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
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.FlexibleGene
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.ArchiveImpactSelector
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GeneTest {

    private class EmptyAdaptiveSelectionObjectGene(
        name: String,
        fields: List<Gene>
    ) : ObjectGene(name, fields) {
        override fun adaptiveSelectSubsetToMutate(
            randomness: Randomness,
            internalGenes: List<Gene>,
            mwc: MutationWeightControl,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo
        ): List<Pair<Gene, AdditionalGeneMutationInfo?>> = emptyList()
    }

    private data class MutationHarness(
        val config: EMConfig,
        val randomness: Randomness,
        val apc: AdaptiveParameterControl,
        val mwc: MutationWeightControl,
        val ags: ArchiveImpactSelector,
        val agm: ArchiveGeneMutator,
        val dummyEvi: EvaluatedIndividual<ConstantIndividual>
    ) {
        fun createInfo(rootGene: Gene, impact: GeneImpact?): AdditionalGeneMutationInfo =
            AdditionalGeneMutationInfo(
                selection = GeneMutationSelectionMethod.APPROACH_IMPACT,
                impact = impact,
                geneReference = rootGene.name,
                archiveGeneSelector = ags,
                archiveGeneMutator = agm,
                evi = dummyEvi,
                targets = setOf(1),
                localId = null,
                rootGene = rootGene
            )
    }

    private fun createHarness(): MutationHarness {
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
        return MutationHarness(config, randomness, apc, mwc, ags, agm, dummyEvi)
    }

    @Test
    fun testSelectSubsetToMutateDefaultSelectsOneChildWithoutAdaptiveSelection() {
        val harness = createHarness()
        val b1 = BooleanGene("b1", false)
        val b2 = BooleanGene("b2", false)
        val b3 = BooleanGene("b3", false)
        val objParent = ObjectGene("parent", listOf(b1, b2, b3)).apply {
            doInitialize(harness.randomness)
        }
        b1.value = false
        b2.value = false
        b3.value = false

        objParent.standardMutation(
            randomness = harness.randomness,
            apc = harness.apc,
            mwc = harness.mwc,
            childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.DEFAULT,
            additionalGeneMutationInfo = harness.createInfo(objParent, ObjectGeneImpact("objImpact", objParent))
        )
        assertEquals(1, listOf(b1, b2, b3).count { it.value })

        val flexChild = BooleanGene("flexChild", false)
        val flexParent = FlexibleGene("flexParent", flexChild, null).apply {
            doInitialize(harness.randomness)
        }
        flexChild.value = false

        flexParent.standardMutation(
            randomness = harness.randomness,
            apc = harness.apc,
            mwc = harness.mwc,
            childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.DEFAULT,
            additionalGeneMutationInfo = harness.createInfo(flexParent, GeneImpact("flexImpact"))
        )
        assertTrue(flexChild.value)
    }

    @Test
    fun testSelectSubsetToMutateDeterministicWeightUsesStaticWeightsAndHypermutation() {
        val harness = createHarness()
        harness.config.d = 0.0
        harness.config.startingPerOfGenesToMutate = 0.5

        val heavyChildren = (1..10).map { BooleanGene("h$it", false) }
        val heavy = ObjectGene("heavy", heavyChildren)
        val light = BooleanGene("light", false)
        val weightedParent = PairGene("weightedParent", heavy, light).apply {
            doInitialize(harness.randomness)
        }

        var heavyMutations = 0
        var lightMutations = 0
        repeat(30) {
            heavyChildren.forEach { it.value = false }
            light.value = false
            weightedParent.standardMutation(
                randomness = harness.randomness,
                apc = harness.apc,
                mwc = harness.mwc,
                childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.DETERMINISTIC_WEIGHT,
                additionalGeneMutationInfo = harness.createInfo(weightedParent, GeneImpact("pairImpact"))
            )
            if (heavyChildren.any { it.value }) heavyMutations++
            if (light.value) lightMutations++
        }
        assertTrue(heavyMutations > lightMutations)
        assertTrue(heavyMutations > 15)
        assertTrue(lightMutations < 10)

        harness.config.d = 1.0
        harness.config.startingPerOfGenesToMutate = 1.0
        val h1 = BooleanGene("h1", false)
        val h2 = BooleanGene("h2", false)
        val hyperParent = PairGene("hyper", h1, h2).apply {
            doInitialize(harness.randomness)
        }
        h1.value = false
        h2.value = false

        hyperParent.standardMutation(
            randomness = harness.randomness,
            apc = harness.apc,
            mwc = harness.mwc,
            childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.DETERMINISTIC_WEIGHT,
            additionalGeneMutationInfo = null
        )
        assertTrue(h1.value)
        assertTrue(h2.value)
    }

    @Test
    fun testSelectSubsetToMutateAdaptiveWeightNullInfoAndNullImpactFallback() {
        val harness = createHarness()
        harness.config.d = 1.0
        harness.config.startingPerOfGenesToMutate = 1.0

        val n1 = BooleanGene("n1", false)
        val n2 = BooleanGene("n2", false)
        val nestedFlex = FlexibleGene("nestedFlex", n1, null)
        val parent = ObjectGene("parent", listOf(nestedFlex, n2)).apply {
            doInitialize(harness.randomness)
        }

        assertThrows<IllegalArgumentException> {
            parent.standardMutation(
                randomness = harness.randomness,
                apc = harness.apc,
                mwc = harness.mwc,
                childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT,
                additionalGeneMutationInfo = null
            )
        }

        n1.value = false
        n2.value = false
        parent.standardMutation(
            randomness = harness.randomness,
            apc = harness.apc,
            mwc = harness.mwc,
            childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT,
            additionalGeneMutationInfo = harness.createInfo(parent, impact = null)
        )
        assertTrue(n1.value)
        assertTrue(n2.value)
    }

    @Test
    fun testSelectSubsetToMutateAdaptiveWeightDelegatesAndValidatesSelection() {
        val harness = createHarness()

        val unsupportedParent = FlexibleGene("unsupported", BooleanGene("b1", false), null).apply {
            doInitialize(harness.randomness)
        }
        val unsupportedEx = assertThrows<IllegalStateException> {
            unsupportedParent.standardMutation(
                randomness = harness.randomness,
                apc = harness.apc,
                mwc = harness.mwc,
                childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT,
                additionalGeneMutationInfo = harness.createInfo(unsupportedParent, GeneImpact("flexImpact"))
            )
        }
        assertTrue(unsupportedEx.message!!.contains("adaptive gene selection is unavailable"))

        val emptyParent = EmptyAdaptiveSelectionObjectGene("empty", listOf(BooleanGene("b1", false))).apply {
            doInitialize(harness.randomness)
        }
        val emptyEx = assertThrows<IllegalStateException> {
            emptyParent.standardMutation(
                randomness = harness.randomness,
                apc = harness.apc,
                mwc = harness.mwc,
                childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT,
                additionalGeneMutationInfo = harness.createInfo(emptyParent, ObjectGeneImpact("emptyImpact", emptyParent))
            )
        }
        assertTrue(emptyEx.message!!.contains("none is selected to mutate"))

        val mismatchedTarget = ObjectGene("mismatch", listOf(StringGene("f1", "abc"))).apply {
            doInitialize(harness.randomness)
        }
        val wrongImpact = ObjectGeneImpact("wrongImpact", ObjectGene("mismatch", listOf(IntegerGene("f1", 1))))
        val mismatchEx = assertThrows<IllegalStateException> {
            mismatchedTarget.standardMutation(
                randomness = harness.randomness,
                apc = harness.apc,
                mwc = harness.mwc,
                childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT,
                additionalGeneMutationInfo = harness.createInfo(mismatchedTarget, wrongImpact)
            )
        }
        assertTrue(mismatchEx.message!!.startsWith("mismatched impact for gene"))

        val validChild = BooleanGene("b1", false)
        val validParent = ObjectGene("validParent", listOf(validChild)).apply {
            doInitialize(harness.randomness)
        }
        validChild.value = false
        validParent.standardMutation(
            randomness = harness.randomness,
            apc = harness.apc,
            mwc = harness.mwc,
            childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT,
            additionalGeneMutationInfo = harness.createInfo(validParent, ObjectGeneImpact("validImpact", validParent))
        )
        assertTrue(validChild.value)
    }

    @Test
    fun testHasSameValueWithTypeCheck() {
        val base = StringGene("field", "42")
        val same = StringGene("field", "42")
        val diffValue = StringGene("field", "99")
        val diffNameSameValue = StringGene("other", "42")
        val diffNameDiffValue = StringGene("other", "99")
        val diffTypeInt = IntegerGene("field", 42)
        val diffTypeHolder = ImmutableDataHolderGene("field", "42", inQuotes = false)

        assertTrue(base.hasSameValueWithTypeCheck(same))
        assertFalse(base.hasSameValueWithTypeCheck(diffValue))

        assertFalse(base.hasSameValueWithTypeCheck(diffNameSameValue))
        assertTrue(base.hasSameValueWithTypeCheck(diffNameSameValue, checkName = false))
        assertFalse(base.hasSameValueWithTypeCheck(diffNameDiffValue, checkName = false))

        assertFalse(base.hasSameValueWithTypeCheck(diffTypeInt))
        assertFalse(base.hasSameValueWithTypeCheck(diffTypeInt, checkName = false))
        assertFalse(base.hasSameValueWithTypeCheck(diffTypeHolder))
        assertFalse(base.hasSameValueWithTypeCheck(diffTypeHolder, checkName = false))
    }
}


