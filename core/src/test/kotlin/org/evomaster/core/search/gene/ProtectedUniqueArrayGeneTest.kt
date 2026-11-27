package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.ProtectedUniqueArrayGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ProtectedUniqueArrayGeneTest {

    @Test
    fun protectedElementsSurviveRepeatedRandomizeAndShallowMutateAndSetNeverBecomesEmpty() {
        val rand = Randomness().apply { updateSeed(2026L) }
        val injector: Injector = LifecycleInjector.builder()
            .withModules(*arrayOf<Module>(BaseModule(arrayOf("--blackBox", "false"), true)))
            .build().createInjector()
        injector.getInstance(EMConfig::class.java).apply {
            stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            maxEvaluations = 1000
            seed = 2026
            weightBasedMutationRate = true
        }
        val apc = injector.getInstance(AdaptiveParameterControl::class.java)
        val mwc = injector.getInstance(MutationWeightControl::class.java)

        val roles = ProtectedUniqueArrayGene(
            name = "roles",
            template = StringGene("role_elem", minLength = 1, maxLength = 12),
            protectedCount = 2,
            minSize = 1,
            maxSize = 6,
            elements = mutableListOf(
                StringGene("role_0", "captain", minLength = 1, maxLength = 12),
                StringGene("role_1", "forward", minLength = 1, maxLength = 12)
            )
        )
        roles.markAllAsInitialized()
        val protectedFirst = roles.getViewOfElements()[0]
        val protectedSecond = roles.getViewOfElements()[1]

        repeat(100) {
            roles.randomize(rand, false)
            assertTrue(roles.getViewOfElements().size >= 2)
            assertTrue(roles.getViewOfElements()[0] === protectedFirst)
            assertTrue(roles.getViewOfElements()[1] === protectedSecond)
            assertTrue(roles.isLocallyValid())
        }

        repeat(200) {
            roles.standardMutation(
                randomness = rand,
                apc = apc,
                mwc = mwc
            )
            assertTrue(roles.getViewOfElements().size >= 2)
            assertTrue(roles.getViewOfElements().size <= 6)
            assertTrue(roles.getViewOfElements()[0] === protectedFirst)
            assertTrue(roles.getViewOfElements()[1] === protectedSecond)
            assertTrue(roles.isLocallyValid())
        }

        // Direct removal of protected element is ignored
        roles.removeExistingElement(protectedFirst)
        assertTrue(roles.getViewOfElements()[0] === protectedFirst)
        assertTrue(roles.getViewOfElements().isNotEmpty())
    }

    @Test
    fun standardMutationPreservesValidityAndProtectedSizeWhenDomainSaturated() {
        val injector: Injector = LifecycleInjector.builder()
            .withModules(*arrayOf<Module>(BaseModule(arrayOf("--blackBox", "false"), true)))
            .build().createInjector()
        injector.getInstance(EMConfig::class.java).apply {
            stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            maxEvaluations = 100
            seed = 666
            weightBasedMutationRate = true
        }
        val rand = injector.getInstance(Randomness::class.java).apply { updateSeed(666) }
        val apc = injector.getInstance(AdaptiveParameterControl::class.java)
        val mwc = injector.getInstance(MutationWeightControl::class.java)

        val positions = listOf("GK", "MF", "FW")
        val lineup = ProtectedUniqueArrayGene(
            name = "lineup",
            template = EnumGene("position", positions),
            protectedCount = 3,
            maxSize = 3,
            elements = mutableListOf(
                EnumGene("pos1", positions, 0),
                EnumGene("pos2", positions, 1),
                EnumGene("pos3", positions, 2)
            )
        ).apply { doInitialize() }

        for (strategy in listOf(
            SubsetGeneMutationSelectionStrategy.DEFAULT,
            SubsetGeneMutationSelectionStrategy.DETERMINISTIC_WEIGHT
        )) {
            repeat(30) {
                lineup.standardMutation(
                    randomness = rand,
                    apc = apc,
                    mwc = mwc,
                    childrenToMutateSelectionStrategy = strategy
                )
                assertTrue(lineup.isLocallyValid())
                val values = lineup.getViewOfElements().map { it.getValueAsRawString() }
                assertEquals(3, values.size)
                assertEquals(setOf("GK", "MF", "FW"), values.toSet())
            }
        }
    }

    @Test
    fun bigDecimalNumericalEquivalenceDeduplicatedRejectedAndRepaired() {
        val initial = ProtectedUniqueArrayGene(
            name = "jerseyNumbers",
            template = BigDecimalGene("num_elem"),
            protectedCount = 2,
            maxSize = 5,
            elements = mutableListOf(
                BigDecimalGene("n_0", BigDecimal("10.5")),
                BigDecimalGene("n_1", BigDecimal("10.50")),
                BigDecimalGene("n_2", BigDecimal("7"))
            )
        )
        assertEquals(2, initial.getViewOfElements().size)
        assertEquals(2, initial.protectedCount)
        assertTrue(initial.isLocallyValid())

        assertThrows(IllegalArgumentException::class.java) {
            initial.addElement(BigDecimalGene("dup", BigDecimal("10.50")))
        }

        val trailing = BigDecimalGene("n_3", BigDecimal("9"))
        initial.addElement(trailing)
        assertEquals(3, initial.getViewOfElements().size)

        // Direct in-place leaf mutation into a duplicate makes isLocallyValid() == false
        trailing.value = BigDecimal("10.50")
        assertFalse(initial.isLocallyValid())

        // repair() removes the unprotected trailing duplicate and restores validity
        initial.repair()
        assertEquals(2, initial.getViewOfElements().size)
        assertTrue(initial.isLocallyValid())
    }

    @Test
    fun crossTypeSymmetryHoldsBetweenArrayGeneAndProtectedUniqueArrayGene() {
        val protectedSet = ProtectedUniqueArrayGene(
            name = "countries",
            template = StringGene("c_elem"),
            protectedCount = 1,
            elements = mutableListOf(StringGene("c_0", "Argentina"))
        )
        val plainArray = ArrayGene(
            name = "countries",
            template = StringGene("c_elem"),
            uniqueElements = true,
            elements = mutableListOf(StringGene("c_0", "Argentina"))
        )

        assertFalse(protectedSet.containsSameValueAs(plainArray))
        assertFalse(plainArray.containsSameValueAs(protectedSet))
        assertFalse(protectedSet.unsafeCopyValueFrom(plainArray))
        assertFalse(plainArray.unsafeCopyValueFrom(protectedSet))
        assertEquals(1, protectedSet.getViewOfElements().size)
    }
}
