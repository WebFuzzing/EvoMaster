package org.evomaster.core.output.naming

import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import kotlin.reflect.KFunction1

class NamingHelper {
    /**
     * The presence of a call with a 500 status code will be added to the test name.
     */
    private fun criterion1_500 (individual: EvaluatedIndividual<*>): String{
        if (individual.seeResults().filterIsInstance<HttpWsCallResult>().any{ it.getStatusCode() == 500 }){
            return "_with500"
        }
        return ""
    }

    private fun criterion2_hasPost (individual: EvaluatedIndividual<*>): String{
        if(individual.individual.seeAllActions().filterIsInstance<RestCallAction>().any{it.verb == HttpVerb.POST} ){
            return "_hasPost"
        }

        return ""
    }

    /**
     * The type of sample is added to the name. This is tied to the the [RestIndividual] and will change with a new problem.
     */
    private fun criterion3_sampling(individual: EvaluatedIndividual<*>): String{
        if(individual.individual is RestIndividual)
            return "_" + (individual.individual as RestIndividual).sampleType
        else return ""
    }

    /**
     * The presence of separate steps for DB initialization will be added to the test name. This is currently tied to
     * the [RestIndividual] and will change with a new problem
     */
    private fun criterion4_dbInit(individual: EvaluatedIndividual<*>): String{
        if ((individual.individual is RestIndividual) && (individual.individual as RestIndividual).seeInitializingActions().isNotEmpty()){
            return "_" + "hasDbInit"
        }
        else return ""
    }

//    private fun criterion5_partialOracle(individual: EvaluatedIndividual<*>): String{
//        var name = ""
//        partialOracles.adjustName().forEach {
//            if(!it.adjustName().isNullOrBlank()
//                    && it.generatesExpectation(individual)){
//                name = name + it.adjustName()
//            }
//        }
//        return name
//    }

//    fun setPartialOracles(partialOracles: PartialOracles){
//        this.partialOracles = partialOracles
//    }

    //    private var partialOracles = PartialOracles()
    private var namingCriteria =  listOf(::criterion1_500 ) //, ::criterion5_partialOracle)
    private val availableCriteria = listOf(::criterion1_500, ::criterion2_hasPost, ::criterion3_sampling, ::criterion4_dbInit) //, ::criterion5_partialOracle)


    fun suggestName(individual: EvaluatedIndividual<*>): String{
        return namingCriteria.map { it(individual) }.joinToString("")
    }

    fun getAvailableCriteria(): List<KFunction1<EvaluatedIndividual<*>, String>> {
        return availableCriteria
    }

    fun selectCriteria(selected: List<KFunction1<EvaluatedIndividual<*>, String>>){
        if (availableCriteria.containsAll(selected)){
            namingCriteria = selected
        }
        else {
            throw UnsupportedOperationException("The naming criteria chosen appear to not be supported at the moment.")
        }
    }

    fun selectCriteriaByIndex(selected: List<Int>){
        if (availableCriteria.indices.toList().containsAll(selected)){
            for (i in selected)
                namingCriteria = availableCriteria.filterIndexed{ index, _ ->
                    selected.contains(index)
                } as List<KFunction1<EvaluatedIndividual<*>, String>>
        }
        else {
            throw UnsupportedOperationException("The naming criteria chosen appear to not be supported at the moment.")
        }
    }


}
