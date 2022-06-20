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
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.StandardMutator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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
    override fun getSampler(): AbstractRestSampler = sampler



    override fun initService(injector: Injector) {

        sampler = injector.getInstance(ResourceSampler::class.java)
        mutator = injector.getInstance(ResourceRestMutator::class.java)

        rm = injector.getInstance(ResourceManageService::class.java)
        ff = injector.getInstance(RestResourceFitness::class.java)

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