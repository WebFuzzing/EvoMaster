package org.evomaster.core.search.service.mutator.genemutation

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.algorithms.constant.ConstantIndividual
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.impact.impactinfocollection.value.StringGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArchiveGeneMutatorSelectSpecTest {

    @Test
    fun testSelectSpecWithMatchingAndMismatchedSpecializationImpacts() {
        val injector: Injector = LifecycleInjector.builder()
            .withModules(*arrayOf<Module>(BaseModule(arrayOf("--blackBox", "false"), true)))
            .build().createInjector()
        val config = injector.getInstance(EMConfig::class.java)
        config.seed = 42
        val randomness = injector.getInstance(Randomness::class.java).apply { updateSeed(42) }
        val agm = injector.getInstance(ArchiveGeneMutator::class.java)
        val mwc = injector.getInstance(MutationWeightControl::class.java)

        val dummyInd = ConstantIndividual(IntegerGene("dummy", 0)).also {
            TestUtils.doInitializeIndividualForTesting(it, randomness)
        }
        val dummyEvi = EvaluatedIndividual(FitnessValue(1.0), dummyInd, listOf())

        // 1. Existing behavior: equal specialization impact count and specializationGenes size
        val geneMatching = StringGene(
            "s",
            "2020-01-01",
            specializationGenes = mutableListOf(DateGene("d1"), DateGene("d2"))
        ).apply { doInitialize(randomness) }
        val impactMatching = StringGeneImpact("sId", geneMatching)
        val specMethod = ArchiveGeneMutator::class.java.getDeclaredMethod(
            "selectSpec",
            StringGene::class.java,
            StringGeneImpact::class.java,
            Set::class.java
        ).apply { isAccessible = true }

        val idxMatch = specMethod.invoke(agm, geneMatching, impactMatching, setOf(1)) as Int
        assertTrue(idxMatch in 0 until geneMatching.specializationGenes.size)

        // 2. Bug fix: gene has more specializationGenes than impact has specializationImpacts
        // Previously `(size - 1 until specSize)` caused index mismatch / IndexOutOfBoundsException when size == 0
        val geneExtraSpec = StringGene(
            "s2",
            "2020-01-01",
            specializationGenes = mutableListOf(DateGene("d1"), DateGene("d2"), DateGene("d3"))
        ).apply { doInitialize(randomness) }
        val impactFewer = StringGeneImpact("s2Id", StringGene("s2", "x", specializationGenes = mutableListOf(DateGene("d1"))))
        val idxExtra = specMethod.invoke(agm, geneExtraSpec, impactFewer, setOf(1)) as Int
        assertTrue(idxExtra in 0 until (geneExtraSpec.specializationGenes.size - impactFewer.getSpecializationImpacts().size))

        // 3. Bug fix: impact has 0 specializationImpacts while gene has specializationGenes
        val impactEmpty = StringGeneImpact("s3Id", StringGene("s3", "x"))
        val idxEmpty = specMethod.invoke(agm, geneExtraSpec, impactEmpty, setOf(1)) as Int
        assertTrue(idxEmpty in 0 until geneExtraSpec.specializationGenes.size)
        assertTrue(mwc != null && dummyEvi != null)
    }
}
