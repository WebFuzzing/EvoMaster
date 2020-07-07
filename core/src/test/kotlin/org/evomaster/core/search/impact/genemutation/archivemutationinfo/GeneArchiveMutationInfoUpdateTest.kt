package org.evomaster.core.search.impact.genemutation.archivemutationinfo

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.geneMutation.archive.IntegerGeneArchiveMutationInfo
import org.evomaster.core.search.service.mutator.geneMutation.archive.StringGeneArchiveMutationInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


/**
 * created by manzh on 2020-06-16
 */
class GeneArchiveMutationInfoUpdateTest {


    private lateinit var config: EMConfig
    private lateinit var time : SearchTimeController
    private lateinit var apc: AdaptiveParameterControl
    private lateinit var archiveMutator: ArchiveGeneMutator
    private lateinit var randomness: Randomness

    @BeforeEach
    fun init(){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(BaseModule()))
                .build().createInjector()
        randomness = injector.getInstance(Randomness::class.java)
        config = injector.getInstance(EMConfig::class.java)
        time = injector.getInstance(SearchTimeController::class.java)
        apc = injector.getInstance(AdaptiveParameterControl::class.java)
        archiveMutator = injector.getInstance(ArchiveGeneMutator::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
        config.focusedSearchActivationTime = 0.5
        config.maxActionEvaluations = 10
        config.probOfArchiveMutation = 1.0
        config.archiveGeneMutation = EMConfig.ArchiveGeneMutation.SPECIFIED

    }

    @Test
    fun testStringGeneArchiveCharMutationUpdate(){
        val f = 'f'
        val f2 = f.toInt() + 1

        val foo = StringGene(name = "foo", value = "${f}oo")
        val f2oo = StringGene(name = "foo", value = "${f2.toChar()}oo")

        val target = 9

        // mutated is not this. from foo to f2oo, fitness becomes worse, ie, -1 for targets
        foo.archiveMutationUpdate(original = foo, mutated = f2oo, targetsEvaluated = mutableMapOf(target to EvaluatedMutation.WORSE_THAN), archiveMutator = archiveMutator)
        assertEquals(1, foo.mutationInfo.map.size)

        val gmi = foo.mutationInfo.map[target] as? StringGeneArchiveMutationInfo

        gmi.apply {
            assertNotNull(this)
            assertEquals(0, this!!.mutatedIndex)
            assertEquals(3, charsMutation.size)
            assert(charsMutation[0].preferMax < f2)
        }

        val f3 = f.toInt() - 1
        val f3oo = foo.copy() as StringGene
        f3oo.value = "${f3.toChar()}oo"

        //mutated is this. from foo to f3oo, fitness becomes worse
        f3oo.archiveMutationUpdate(original = foo, mutated = f3oo, targetsEvaluated = mutableMapOf(target to EvaluatedMutation.WORSE_THAN), archiveMutator = archiveMutator)
        f3oo.apply {
            assertEquals(1, mutationInfo.map.size)
            assertNotNull(mutationInfo.map[target])
            assert((mutationInfo.map[target]!! as StringGeneArchiveMutationInfo).charsMutation[0].preferMin > f3)
            assert((mutationInfo.map[target]!! as StringGeneArchiveMutationInfo).charsMutation[0].preferMax < f2)
            assert((mutationInfo.map[target]!! as StringGeneArchiveMutationInfo).charsMutation[0].reached)
        }
    }


    @Test
    fun testStringGeneArchiveLengthMutationUpdate(){

        val foo = StringGene(name = "foo", value = "f")
        val f2oo = StringGene(name = "foo", value = "fo")

        val target = 9

        // mutated is not this. from foo to f2oo, fitness becomes worse, ie, -1 for targets
        foo.archiveMutationUpdate(original = foo, mutated = f2oo, targetsEvaluated = mutableMapOf(target to EvaluatedMutation.WORSE_THAN), archiveMutator = archiveMutator)
        assertEquals(1, foo.mutationInfo.map.size)

        (foo.mutationInfo.map[target] as? StringGeneArchiveMutationInfo).apply {
            assertNotNull(this)
            assertEquals(-1, this!!.mutatedIndex)
            assertEquals(1, charsMutation.size)
            assert(lengthMutation.preferMax <= f2oo.value.length)
            assertEquals(foo.minLength, lengthMutation.preferMin)
        }

        val f3oo = foo.copy() as StringGene
        f3oo.value = ""

        f3oo.archiveMutationUpdate(original = foo, mutated = f3oo, targetsEvaluated = mutableMapOf(target to EvaluatedMutation.BETTER_THAN), archiveMutator = archiveMutator)
        (f3oo.mutationInfo.map[target] as? StringGeneArchiveMutationInfo).apply {
            assertNotNull(this)
            assertEquals(-1, this!!.mutatedIndex)
            assertEquals(0, charsMutation.size)
            assertEquals(0, lengthMutation.preferMax)
            assertEquals(f3oo.minLength, lengthMutation.preferMin)
            assert(lengthMutation.reached)
        }
        assert(f3oo.reachOptimal(setOf(9)))
    }

    @Test
    fun testIntegerGeneArchiveMutation(){
        val i1 = IntegerGene("foo", value = 1)

        val target = 9
        val i10 = i1.copy() as IntegerGene
        i10.value = 10

        i1.archiveMutationUpdate(i1, i10, targetsEvaluated = mutableMapOf(target to EvaluatedMutation.WORSE_THAN), archiveMutator = archiveMutator)

        (i1.mutationInfo.map[target] as? IntegerGeneArchiveMutationInfo).apply {
            assertNotNull(this)
            assert(this!!.valueMutation.preferMax <= i10.value)
            assertEquals(i1.min, valueMutation.preferMin)
        }

        val im1000 = i1.copy() as IntegerGene
        im1000.value = -1000

        (im1000.mutationInfo.map[target] as? IntegerGeneArchiveMutationInfo).apply {
            assertNotNull(this)
            assertEquals((i1.mutationInfo.map[target] as IntegerGeneArchiveMutationInfo).valueMutation.preferMax, this!!.valueMutation.preferMax)
            assertEquals((i1.mutationInfo.map[target] as IntegerGeneArchiveMutationInfo).valueMutation.preferMin, valueMutation.preferMin)
        }

        im1000.archiveMutationUpdate(i1, im1000, targetsEvaluated = mutableMapOf(target to EvaluatedMutation.BETTER_THAN), archiveMutator = archiveMutator)
        (im1000.mutationInfo.map[target] as? IntegerGeneArchiveMutationInfo)?.apply {
            assertNotNull(this)
            assert(valueMutation!!.preferMax <=  (im1000.value + i1.value)/2 + 1)
            assertEquals((i1.mutationInfo.map[target] as IntegerGeneArchiveMutationInfo).valueMutation.preferMin, valueMutation.preferMin)
        }
    }
}