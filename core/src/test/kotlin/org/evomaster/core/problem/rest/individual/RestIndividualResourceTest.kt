package org.evomaster.core.problem.rest.individual

import com.google.inject.*
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
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


    override fun extraMutatedIndividualCheck(evaluated: Int, copyOfImpact: ImpactsOfIndividual?,
                                             original: EvaluatedIndividual<RestIndividual>, mutated: EvaluatedIndividual<RestIndividual>) {
        checkTracking(evaluated + 1, mutated)

        checkImpactUpdate(evaluated, copyOfImpact, original, mutated)
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

    private fun checkImpactUpdate(evaluated: Int, copyOfImpact: ImpactsOfIndividual?,
                                  original: EvaluatedIndividual<RestIndividual>, mutated: EvaluatedIndividual<RestIndividual>){

        assertNotNull(mutated)
        assertNotNull(mutated.impactInfo)
        val existingData = mutated.impactInfo!!.getSQLExistingData()
        assertEquals(existingData, mutated.individual.seeInitializingActions().count { it.representExistingData })

        val anyNewDbActions = mutated.individual.seeInitializingActions().size - original.individual.seeInitializingActions().size

        if (anyNewDbActions == 0){
            if (mutated.trackOperator?.operatorTag() == RestResourceStructureMutator::class.java.simpleName){
                //TODO
            }else if (mutated.trackOperator?.operatorTag() == ResourceRestMutator::class.java.simpleName){
                //TODO

            }else{
                fail("the operator (${mutated.trackOperator?.operatorTag()?:"null"}) is not expected")
            }
        }else if (anyNewDbActions > 0){
            // impact structure should be updated
        } else{
            fail("DbAction should not be removed with the current strategy for REST problem")
        }

        if (searchTimeController.evaluatedActions > 20 || searchTimeController.percentageUsedBudget() >= 0){
            /*
                newly additional dbaction would affect the impact collections
                then disable after 10% used budget
             */
            employFakeDbHeuristicResult = false
        }


    }
}