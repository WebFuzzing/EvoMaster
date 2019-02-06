package org.evomaster.core.problem.rest2.resources

import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.WithGet
import org.evomaster.core.problem.rest.serviceII.resources.RestAResource
import org.evomaster.core.problem.rest.serviceII.resources.RestResource
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls


/**
 * [restB] relies on [restA]
 * @property executed presents whether a sequence of (restA, restB) has been executed.
 * @property relevantValue is a degree of their relevant
 * @property irrelevantTimes presents times that resources are irrelevant
 */
class RelevantResource(
        val restA : RestAResource,
        val restB : RestAResource) {

    var executed: Boolean = false
        private set
    var relevantValue : Double = 0.0
        private set
    var irrelevantTimes : Int = 0
        private set
    var relevantTimes : Int = 0
        private set
    var onlyB : Int = 0
        private set


    fun isB(b: RestAResource):Boolean{
        return restB.path.toString() == b.path.toString()
    }

    fun checkRelevant(individual: RestIndividualII, changed: Boolean){

        val indexOfA = individual.getResourceCalls().map { it.resource.ar.path.toString() }.indexOf(restA.path.toString())
        val indexOfB = individual.getResourceCalls().map { it.resource.ar.path.toString() }.indexOf(restB.path.toString())

        if(indexOfB < indexOfA || indexOfB == -1) return

        val b : RestResourceCalls = individual.getResourceCalls().first { it.resource.ar.path.toString() == restB.path.toString() }
        if(indexOfA != -1){
            if(executed && changed && !b.seeGenes().any { it.mutated }) reportOnlyB()
        }else{
            val a : RestResourceCalls = individual.getResourceCalls().first { it.resource.ar.path.toString() == restA.path.toString() }
            if(a.getVerbs().last() != HttpVerb.DELETE){
                if(!executed){
                    executed = true
                    return
                }
                if(a.seeGenes().any { it.mutated }){
                    reportRelevant(changed)
                }
            }
        }
    }

    private fun reportRelevant(changed: Boolean){
        if(changed) relevantTimes ++
        else irrelevantTimes++
    }

    private fun reportOnlyB(){
        onlyB++
    }

    private fun getResources():List<RestAResource>{
        return listOf(restA, restB)
    }
}