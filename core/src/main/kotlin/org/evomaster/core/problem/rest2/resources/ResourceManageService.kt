package org.evomaster.core.problem.rest2.resources

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.resources.RestAResource
import org.evomaster.core.search.Action

class ResourceManageService {

    private var initialized = false
    private val resourceCluster : MutableMap<String, RestAResource> = mutableMapOf()
    private val relevants : MutableList<RelevantResource> = mutableListOf()

    fun initAbstractResources(actionCluster : MutableMap<String, Action>){
        if(!initialized){
            actionCluster.values.forEach { u ->
                if(u is RestCallAction){
                    resourceCluster.getOrPut(u.path.toString()){ RestAResource(u.path.copy(), mutableListOf()) }.actions.add(u)
                }
            }

            resourceCluster.values
                    .filterNot { it.independent}
                    .apply {
                        this.forEach { a ->
                            this.forEach { b->
                                if(b != a){
                                    relevants.add(RelevantResource(a, b))
                                }
                            }
                        }
                    }
        }
    }

    fun getRestAResource(key : String) : RestAResource?{
        return resourceCluster.get(key)
    }

    fun getResourceCluster() : Map<String, RestAResource> {
        return resourceCluster.toMap()
    }

    fun allActionExecuted(time : Int) : Boolean{
        resourceCluster.values.forEach {
            if(!it.doesExecuteEveryAction(time)) return false
        }
        return true
    }

    fun allTemplateExecuted(time : Int) : Boolean{
        resourceCluster.values.forEach {
            if(!it.doesExecuteEveryTemplate(time)) return false
        }
        return true
    }

    fun onlyIndependentResource() : Boolean? = if (initialized) resourceCluster.values.filter{ r -> !r.independent }.isEmpty() else null

    fun updateRelevants(ind : RestIndividualII, changed : Boolean){
        relevants.forEach { it.checkRelevant(ind,changed)}
    }

    fun getRelevants(restAResource: RestAResource) : List<RestAResource>{
        return relevants.filter { it.isB(restAResource) && it.relevantTimes > 0}.sortedBy { it.relevantTimes }.map { it.restA }
    }
}