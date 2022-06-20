package org.evomaster.core.problem.rest.individual

import com.google.inject.*
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.StandardMutator
import org.junit.jupiter.api.Assertions.*
import kotlin.math.min

/**
 * tests are for checking resource-based solution with enabled hypermutation
 */
class RestIndividualResourceTest : RestIndividualTestBase(){

    private lateinit var sampler : ResourceSampler
    private lateinit var mutator : ResourceRestMutator
    private lateinit var rm : ResourceManageService
    private lateinit var ff : RestResourceFitness

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


    override fun extraMutatedIndividualCheck(evaluated: Int, original: EvaluatedIndividual<RestIndividual>, mutated: EvaluatedIndividual<RestIndividual>) {
        checkTracking(evaluated + 1, mutated)

        // TODO add some assertions for impact
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