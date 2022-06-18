package org.evomaster.core.problem.rest.individual

import com.google.inject.*
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.resource.ResourceCluster
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.StandardMutator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.min

@Disabled
class RestIndividualResourceTest : RestIndividualTestBase(){

    private lateinit var sampler : ResourceSampler
    private lateinit var mutator : ResourceRestMutator
    private lateinit var rm : ResourceManageService
    private lateinit var ff : RestResourceFitness

    private lateinit var cluster : ResourceCluster

    override fun getProblemModule() = ResourceRestModule(false)
    override fun getMutator(): StandardMutator<RestIndividual> = mutator
    override fun getFitnessFunction(): AbstractRestFitness<RestIndividual> = ff

    override fun initService(injector: Injector) {

        sampler = injector.getInstance(ResourceSampler::class.java)
        mutator = injector.getInstance(ResourceRestMutator::class.java)

        rm = injector.getInstance(ResourceManageService::class.java)
        ff = injector.getInstance(RestResourceFitness::class.java)


    }

    override fun getSampler() = sampler

    private fun sampleDbAction(table : Table) : List<DbAction>{
        val actions = sqlInsertBuilder!!.createSqlInsertionAction(table.name)
        return actions.map { it.copy() as DbAction}
    }

    private fun sampleResourceCall(resNode: RestResourceNode? = null): RestResourceCalls {
        val node = resNode?: randomness.choose(cluster.getCluster().values)
        val sampleOption = randomness.nextInt(0, 3)
        return when (sampleOption) {
            0 -> node.sampleOneAction(verb = null, randomness)
            1 -> node.sampleIndResourceCall(randomness, config.maxTestSize)
            2 -> node.sampleAnyRestResourceCalls(randomness, config.maxTestSize)
            3 -> node.sampleRestResourceCalls(randomness.choose(node.getTemplates().values).template, randomness, config.maxTestSize)
            else -> throw IllegalStateException("not support")
        }
    }

    private fun sampleRestIndividual(dbSize : Int, resourceSize: Int): RestIndividual{
        val dbActions = mutableListOf<DbAction>()
        val resoureCalls = mutableListOf<RestResourceCalls>()
        do {
            val table = randomness.choose(cluster.getTableInfo().values)
            dbActions.addAll(sampleDbAction(table))
        }while (dbActions.size < dbSize)


        do {
            val node = randomness.choose(cluster.getCluster().values)
            resoureCalls.add(sampleResourceCall(node))
        }while (resoureCalls.size < resourceSize)
        return RestIndividual(dbInitialization = dbActions, resourceCalls = resoureCalls, sampleType = SampleType.RANDOM)
    }

    @ParameterizedTest
    @MethodSource("getBudgetAndNumOfResource")
    fun testIndividualResourceManipulation(iteration: Int, numResource: Int){
        initResourceNode(numResource, 5)

        config.maxActionEvaluations = iteration
        config.maxTestSize = 20
        (0 until iteration).forEach { _ ->
            val dbSize = randomness.nextInt(1, 15)
            val resourceSize = randomness.nextInt(2, 4)

            val ind = sampleRestIndividual(dbSize, resourceSize)
            assertEquals(dbSize + resourceSize, ind.getViewOfChildren().size)

            val call = sampleResourceCall()

            // add
            val addedIndex = randomness.nextInt(0, resourceSize-1)
            ind.addResourceCall(addedIndex, call)
            assertEquals(dbSize+addedIndex, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize+resourceSize+1, ind.getViewOfChildren().size)

            // remove
            ind.removeResourceCall(addedIndex)
            assertEquals(-1, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize+resourceSize, ind.getViewOfChildren().size)

            // replace
            val original = ind.getIndexedResourceCalls()
            val old = randomness.choose(original.keys)
            val oldRes = original[old]!!
            ind.replaceResourceCall(old, call)
            assertEquals(-1, ind.getViewOfChildren().indexOf(oldRes))
            assertEquals(dbSize + old, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize+resourceSize, ind.getViewOfChildren().size)

            // swap
            val setToSwap = ind.getIndexedResourceCalls()
            val candidates = randomness.choose(setToSwap.keys, 2)
            assertEquals(2, candidates.size)
            val first = setToSwap[candidates.first()]!!
            val second = setToSwap[candidates.last()]!!
            ind.swapResourceCall(candidates.first(), candidates.last())
            assertEquals(dbSize + candidates.first(), ind.getViewOfChildren().indexOf(second))
            assertEquals(dbSize + candidates.last(), ind.getViewOfChildren().indexOf(first))

        }

    }

//    @Disabled // not finish
//    @ParameterizedTest
//    @MethodSource("getBudgetAndNumOfResource")
//    fun testMutatedAHWIndividual(iteration: Int, numResource: Int){
//        initResourceNode(numResource, 5)
//        config.maxActionEvaluations = iteration
//
//        val ind = sampler.sample()
//        var eval = ff.calculateCoverage(ind)
//        assertNotNull(eval)
//
//        var evaluated = 1
//        do{
//            evaluated ++
//            val mutated = mutator.mutateAndSave(1, eval!!, archive)
//           checkTracking(evaluated, mutated)
//
//
//            eval = mutated
//        }while (searchTimeController.shouldContinueSearch())
//    }

    override fun extraMutatedIndividualCheck(evaluated: Int, mutated: EvaluatedIndividual<RestIndividual>) {
        checkTracking(evaluated, mutated)
    }

    private fun checkTracking(evaluated: Int, mutated: EvaluatedIndividual<RestIndividual>){
        mutated.tracking.apply {
            assertNotNull(this)
            assertEquals(min(evaluated, config.maxLengthOfTraces), this!!.history.size)
            assertNotNull(this.history.last().evaluatedResult)
            // with faked remote controller, it should always return better results
            assertEquals(EvaluatedMutation.BETTER_THAN,this.history.last().evaluatedResult)
        }
    }
}