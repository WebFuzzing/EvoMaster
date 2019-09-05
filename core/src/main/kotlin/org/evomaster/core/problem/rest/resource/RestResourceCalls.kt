package org.evomaster.core.problem.rest.resource

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.gene.Gene

/**
 * the class is used to structure actions regarding resources.
 * @property template is a resource template, e.g., POST-GET
 * @property resourceInstance presents a resource that [actions] perform on. [resourceInstance] is an instance of [RestResourceNode]
 * @property actions is a sequence of actions in the [RestResourceCalls] that follows [template]
 */
class RestResourceCalls(
        val template: CallsTemplate? = null,
        val resourceInstance: RestResourceInstance?=null,
        val actions: MutableList<RestAction>
){

    /**
     * [dbActions] are used to initialize data for rest actions, either select from db or insert new data into db
     */
    val dbActions = mutableListOf<DbAction>()

    var status = ResourceStatus.NOT_FOUND

    /**
     * whether the call can be deleted during structure mutation
     */
    var isDeletable = true

    /**
     * this call should be before [shouldBefore]
     */
    var shouldBefore = mutableListOf<String>()

    fun copy() : RestResourceCalls{
        val copy = RestResourceCalls(template, resourceInstance?.copy(), actions.map { a -> a.copy() as RestAction}.toMutableList())
        if(dbActions.isNotEmpty()){
            dbActions.forEach { db->
                copy.dbActions.add(db.copy() as DbAction)
            }
        }

        copy.isDeletable = isDeletable
        copy.shouldBefore.addAll(shouldBefore)

        return copy
    }

    /**
     * @return genes that represents this resource, i.e., longest action in this resource call
     */
    fun seeGenes() : List<out Gene>{
        return longestPath().seeGenes()
    }

    fun repairGenesAfterMutation(gene: Gene? = null){
        val target = longestPath()
        if(gene != null) repairGenePerAction(gene, target)
        else{
            actions.filter { it is RestCallAction && it != target }
                    .forEach{a-> (a as RestCallAction).bindToSamePathResolution(target as RestCallAction)}
        }
    }

    private fun longestPath() : RestAction{
        val max = actions.filter { it is RestCallAction }.asSequence().map { a -> (a as RestCallAction).path.levels() }.max()!!
        val candidates = actions.filter { a -> a is RestCallAction && a.path.levels() == max }
        return candidates.first()
    }

    private fun repairGenePerAction(gene: Gene, action : RestAction){
        val genes = action.seeGenes().flatMap { g->g.flatView() }
        if(genes.contains(gene))
            genes.filter { ig-> ig != gene && ig.name == gene.name && ig::class.java.simpleName == gene::class.java.simpleName }.forEach {cg->
                cg.copyValueFrom(gene)
            }
    }

    fun getResourceNode() : RestResourceNode = resourceInstance?.referResourceNode?:throw IllegalArgumentException("the individual does not have resource structure")

    fun getResourceNodeKey() : String = getResourceNode().getName()

}

enum class ResourceStatus{
    /**
     * DO NOT require resource
     */
    NOT_EXISTING,
    /**
     * resource is created
     */
    CREATED,
    /**
     * require resource, but not enough length for post actions
     */
    NOT_ENOUGH_LENGTH,
    /**
     * require resource, but do not find post action
     */
    NOT_FOUND,
    /**
     * require resource, but post action requires another resource which cannot be created
     */
    NOT_FOUND_DEPENDENT
}