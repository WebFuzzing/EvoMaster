package org.evomaster.core.search.gene

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.collection.HeterogeneousListGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.impact.impactinfocollection.value.collection.HeterogeneousListGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** Unit tests for [HeterogeneousListGene] and [HeterogeneousListGeneImpact]. */
class HeterogeneousListGeneTest {

    @Test
    fun supportsEmptyListsInBothFixedAndMutableModes() {
        val fixedEmpty = HeterogeneousListGene("emptyFixed", emptyList())
        assertTrue(fixedEmpty.isFixed)
        assertTrue(fixedEmpty.elements.isEmpty())
        assertFalse(fixedEmpty.isMutable())
        assertEquals("[]", fixedEmpty.getValueAsRawString())

        val template = ChoiceGene(
            "elem_template",
            listOf(StringGene("s"), BigDecimalGene("n"), BooleanGene("b"))
        )
        val mutableEmpty = HeterogeneousListGene(
            name = "emptyMutable",
            elements = emptyList(),
            isFixed = false,
            template = template,
            minSize = 0,
            maxSize = 5
        )
        assertFalse(mutableEmpty.isFixed)
        assertTrue(mutableEmpty.elements.isEmpty())
        assertTrue(mutableEmpty.isMutable())
        assertNull(template.parent)
        assertEquals("[]", mutableEmpty.getValueAsRawString())
    }

    @Test
    fun elementsLiveViewUpdatesOnShallowMutateAndProtectsMinSizePrefix() {
        val rand = Randomness().apply { updateSeed(1986L) }
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
        val seedPlayer = StringGene("timeline_0", "Argentina")
        val seedGoals = IntegerGene("timeline_1", 3)
        val template = StringGene("timeline_template", minLength = 1, maxLength = 8)

        val list = HeterogeneousListGene(
            name = "matchTimeline",
            elements = listOf(seedPlayer, seedGoals),
            isFixed = false,
            template = template,
            minSize = 2,
            maxSize = 5
        )
        list.markAllAsInitialized()

        assertEquals(2, list.elements.size)
        assertNull(template.parent)

        var sawGrowth = false
        repeat(200) {
            list.standardMutation(
                randomness = rand,
                apc = apc,
                mwc = mwc
            )
            assertTrue(list.elements.size in 2..5)
            assertTrue(list.elements[0] === seedPlayer)
            assertTrue(list.elements[1] === seedGoals)
            assertTrue(list.flatView().all { it.initialized })
            assertNull(template.parent)
            if (list.elements.size > 2) {
                sawGrowth = true
            }
        }
        assertTrue(sawGrowth)
    }

    @Test
    fun rejectsSubclassInstancesAndValidatesImpactType() {
        val hetList = HeterogeneousListGene(
            "stats",
            listOf(StringGene("stats_0", "Messi"), BigDecimalGene("stats_1", BigDecimal("10")))
        )
        val tupleSubclass = TupleGene(
            "stats",
            listOf(StringGene("stats_0", "Messi"), BigDecimalGene("stats_1", BigDecimal("10")))
        )

        assertFalse(hetList.containsSameValueAs(tupleSubclass))
        assertFalse(hetList.unsafeCopyValueFrom(tupleSubclass))

        val hetOtherTypes = HeterogeneousListGene(
            "stats",
            listOf(BigDecimalGene("stats_0", BigDecimal("10")), StringGene("stats_1", "Messi"))
        )
        assertFalse(hetList.containsSameValueAs(hetOtherTypes))

        val listImpact = ImpactUtils.createGeneImpact(hetList, hetList.name)
        assertTrue(listImpact is HeterogeneousListGeneImpact)
        assertTrue(listImpact.validate(hetList))
        assertFalse(listImpact.validate(tupleSubclass))
    }
}
