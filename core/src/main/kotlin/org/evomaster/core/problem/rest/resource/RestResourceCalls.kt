package org.evomaster.core.problem.rest.resource

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.service.RestActionHandlingUtil
import org.evomaster.core.problem.rest.util.RestResourceTemplateHandler
import org.evomaster.core.search.Action
import org.evomaster.core.search.GeneFilter
import org.evomaster.core.search.gene.Gene

/**
 * the class is used to structure actions regarding resources.
 * @property template is a resource template, e.g., POST-GET
 * @property resourceInstance presents a resource that [restActions] perform on. [resourceInstance] is an instance of [RestResourceNode]
 * @property restActions is a sequence of actions in the [RestResourceCalls] that follows [template]
 * @property dbActions are used to initialize data for rest actions, either select from db or insert new data into db
 */
class RestResourceCalls(
    val sampledTemplate : String? = null,
    val template: CallsTemplate? = null,
    val resourceInstance: RestResourceInstance?=null,
    val restActions: MutableList<RestAction>,
    val dbActions : MutableList<DbAction> = mutableListOf()
){

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
        val copy = RestResourceCalls(sampledTemplate, template, resourceInstance?.copy(), restActions.map { a -> a.copy() as RestAction}.toMutableList())
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
    fun seeGenes(filter : GeneFilter = GeneFilter.NO_SQL) : List<out Gene>{
        return when(filter){
            GeneFilter.NO_SQL -> seeRestGenes()
            GeneFilter.ONLY_SQL -> seeSQLGenes()
            GeneFilter.ALL-> seeSQLGenes().plus(seeRestGenes())
            else -> throw IllegalArgumentException("there is no initialization in an ResourceCall")
        }
    }

    fun seeActions() : List<out Action> = dbActions.plus(restActions)

    /**
     * @return the mutable SQL genes and they do not bind with any of Rest Actions
     *
     * FIXME Man: shall we only return mutable genes?
     */
    private fun seeSQLGenes() : List<out Gene> = getResourceNode().getMutableSQLGenes(dbActions, getRestTemplate())

    private fun seeRestGenes() : List<out Gene> = getResourceNode().getMutableRestGenes(restActions, getRestTemplate())

    /**
     * repair binding after mutation
     */
    fun repairGenesAfterMutation(){
        /*
            since we do not mutate the SQL genes which related to Rest Actions,
            we do not need to update rest actions based on mutated SQL genes
         */
        seeRestGenes().map {g->
            val target = restActions.find { it.seeGenes().contains(g) }
                ?:throw IllegalArgumentException("${g.name} cannot be found in any rest actions, and the current actions are ${restActions.joinToString(","){ it.getName() }}")
            val param = (target as? RestCallAction)?.parameters?.find { it.seeGenes().contains(g) }
                ?:throw IllegalStateException("${g.name} cannot be found in rest action ${target.getName()}")
            restActions.filter { it != target }
                .forEach{a-> (a as RestCallAction).bindToSamePathResolution(target.path, listOf(param))}
        }

        // update dbactions based on rest actions
        if (dbActions.isNotEmpty()){
            RestActionHandlingUtil.bindRestActionsWithDbActions(
                dbActions,
                this,
                false
            )
        }
    }

    fun bindRestActionsWith(restResourceCalls: RestResourceCalls){
        if (restResourceCalls.getResourceNode().path != getResourceNode().path)
            throw IllegalArgumentException("target to bind refers to a different resource node, i.e., target (${restResourceCalls.getResourceNode().path}) vs. this (${getResourceNode().path})")
        val params = restResourceCalls.resourceInstance?.params?:restResourceCalls.restActions.filterIsInstance<RestCallAction>().flatMap { it.parameters }
        restActions.forEach { ac ->
            if((ac as RestCallAction).parameters.isNotEmpty()){
                ac.bindToSamePathResolution(ac.path, params)
            }
        }
    }


    /**
     * employing the longest action to represent a group of calls on a resource
     */
    private fun longestPath() : RestAction{
        if (restActions.size == 1) return restActions.first()
        val max = restActions.filter { it is RestCallAction }.asSequence().map { a -> (a as RestCallAction).path.levels() }.max()!!
        val candidates = restActions.filter { a -> a is RestCallAction && a.path.levels() == max }
        return candidates.first()
    }

    private fun repairGenePerAction(gene: Gene, action : RestAction){
        val genes = action.seeGenes().flatMap { g->g.flatView() }
        if(genes.contains(gene))
            genes.filter { ig-> ig != gene && ig.name == gene.name && ig::class.java.simpleName == gene::class.java.simpleName }.forEach {cg->
                cg.copyValueFrom(gene)
            }
    }

    fun getTemplate() : String{
        return sampledTemplate?:RestResourceTemplateHandler.getStringTemplateByCalls(this)
    }


    fun getRestTemplate() = template?.template?:RestResourceTemplateHandler.getStringTemplateByActions(restActions as List<RestCallAction>)

    fun getResourceNode() : RestResourceNode = resourceInstance?.referResourceNode?:throw IllegalArgumentException("the individual does not have resource structure")

    fun getResourceNodeKey() : String = getResourceNode().getName()

    // if the action is bounded with existing data from db, it is not mutable
    fun isMutable() = dbActions.none {
        it.representExistingData
    }

}

enum class ResourceStatus(val value: Int){
    CREATED_SQL(2),
    /**
     * DO NOT require resource
     */
    NOT_EXISTING(1),
    /**
     * resource is created
     */
    CREATED_REST(0),
    /**
     * require resource, but not enough length for post actions
     */
    NOT_ENOUGH_LENGTH(-1),
    /**
     * require resource, but do not find post action
     */
    NOT_FOUND(-2),
    /**
     * require resource, but post action requires another resource which cannot be created
     */
    NOT_FOUND_DEPENDENT(-3)
}