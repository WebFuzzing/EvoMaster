package org.evomaster.core.problem.asyncapi.service

import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver.Companion.replied
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncApiStructureMutatorTest {

    companion object {
        private const val NCS = AsyncApiTestInjector.NCS

        private val NCS_OPERATIONS = AsyncApiTestInjector.NCS_OPERATIONS
    }

    private lateinit var sampler: AsyncApiSampler
    private lateinit var mutator: AsyncApiStructureMutator
    private lateinit var fitness: FitnessFunction<AsyncApiIndividual>

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    private fun start(maxTestSize: Int) {

        val driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(AsyncApiAccess.readFromResource(NCS))) {
            replied("""{"resultAsDouble": 1.0}""")
        }
        val injector = AsyncApiTestInjector.create(driver, "--blackBox=false", "--maxTestSize=$maxTestSize")

        sampler = injector.getInstance(AsyncApiSampler::class.java)
        mutator = injector.getInstance(StructureMutator::class.java) as AsyncApiStructureMutator
        fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))
    }

    private fun evaluate(individual: AsyncApiIndividual): EvaluatedIndividual<AsyncApiIndividual> =
        fitness.calculateCoverage(individual, modifiedSpec = null) ?: fail("the fitness gave up on the individual")

    /**
     * Mutate the structure [times] over, each time from the previous result, and return the
     * sizes seen along the way.
     */
    private fun sizesAlong(times: Int): List<Int> {

        var evaluated = evaluate(sampler.sample(forceRandomSample = true))
        val sizes = mutableListOf<Int>()

        repeat(times) {
            val copy = evaluated.individual.copy() as AsyncApiIndividual
            mutator.mutateStructure(copy, evaluated, null, setOf())
            sizes.add(copy.seeMainExecutableActions().size)
            evaluated = evaluate(copy)
        }

        return sizes
    }

    @Test
    fun testATestKeepsAtLeastOneMessageAndNeverGrowsPastTheMaximum() {

        start(maxTestSize = 2)

        val sizes = sizesAlong(40)

        assertTrue(sizes.all { it in 1..2 }, "sizes seen: $sizes")
        //with room for exactly two, every mutation has to go the other way from the last
        assertEquals(setOf(1, 2), sizes.toSet(), "sizes seen: $sizes")
    }

    @Test
    fun testTheMaximumCanBeReached() {

        start(maxTestSize = 4)

        val sizes = sizesAlong(60)

        assertTrue(sizes.all { it in 1..4 }, "sizes seen: $sizes")
        assertTrue(sizes.contains(4), "the search never got to publish four messages: $sizes")
    }

    @Test
    fun testAnAddedMessageIsOneTheDocumentDeclares() {

        start(maxTestSize = 5)

        var evaluated = evaluate(sampler.sample(forceRandomSample = true))

        repeat(30) {
            val copy = evaluated.individual.copy() as AsyncApiIndividual
            mutator.mutateStructure(copy, evaluated, null, setOf())
            assertTrue(copy.seeMainExecutableActions().all { it.getName() in NCS_OPERATIONS })
            evaluated = evaluate(copy)
        }
    }

    @Test
    fun testWhatWasAddedOrRemovedIsRecordedForTheSearch() {

        /*
            The archive and the impact bookkeeping are fed from this specification, and they are
            shared with the other problem types, so a wrong entry degrades the search silently.
         */
        start(maxTestSize = 4)

        var evaluated = evaluate(sampler.sample(forceRandomSample = true))
        var added = 0
        var removed = 0

        repeat(30) {
            val before = evaluated.individual.seeMainExecutableActions().size
            val copy = evaluated.individual.copy() as AsyncApiIndividual
            val recorded = MutatedGeneSpecification()

            mutator.mutateStructure(copy, evaluated, recorded, setOf())

            val after = copy.seeMainExecutableActions().size
            val grew = after > before

            /*
                One entry per top gene of the action that moved, so what matters is that there is
                at least one and that they all say the same thing.
             */
            val types = recorded.mutatedGenes.map { it.type }.toSet()
            assertEquals(setOf(if (grew) MutatedGeneSpecification.MutatedType.ADD else MutatedGeneSpecification.MutatedType.REMOVE), types, "size went $before -> $after")

            if (grew) added++ else removed++
            evaluated = evaluate(copy)
        }

        //both directions were taken, so neither assertion above passed only by never happening
        assertTrue(added > 0 && removed > 0, "added=$added removed=$removed")
    }

    @Test
    fun testNothingChangesWhenOnlyOneMessageIsAllowed() {

        start(maxTestSize = 1)

        val evaluated = evaluate(sampler.sample(forceRandomSample = true))
        val before = evaluated.individual.seeMainExecutableActions().map { it.getName() }

        val copy = evaluated.individual.copy() as AsyncApiIndividual
        mutator.mutateStructure(copy, evaluated, null, setOf())

        assertEquals(before, copy.seeMainExecutableActions().map { it.getName() })
    }
}
