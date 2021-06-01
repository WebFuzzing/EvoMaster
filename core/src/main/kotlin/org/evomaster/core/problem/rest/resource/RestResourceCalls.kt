package org.evomaster.core.problem.rest.resource

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.rest.util.RestResourceTemplateHandler
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.problem.util.inference.SimpleDeriveResourceBinding
import org.evomaster.core.problem.util.inference.model.ParamGeneBindMap
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual.GeneFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * the class is used to structure actions regarding resources.
 * @property template is a resource template, e.g., POST-GET
 * @property resourceInstance presents a resource that [actions] perform on. [resourceInstance] is an instance of [RestResourceNode]
 * @property actions is a sequence of actions in the [RestResourceCalls] that follows [template]
 * @property dbActions are used to initialize data for rest actions, either select from db or insert new data into db
 */
class RestResourceCalls(
        val template: CallsTemplate? = null,
        val resourceInstance: RestResourceInstance?=null,
        val actions: MutableList<RestCallAction>,
        val dbActions : MutableList<DbAction> = mutableListOf()
){

    companion object{
        private val  log : Logger = LoggerFactory.getLogger(RestResourceCalls::class.java)
    }

    /**
     * presents whether the SQL is
     *      1) for creating missing resources for POST or
     *      2) POST-POST
     */
    var is2POST = false

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
        val copy = RestResourceCalls(template, resourceInstance?.copy(), actions.map { a -> a.copy() as RestCallAction}.toMutableList())
        if(dbActions.isNotEmpty()){
            dbActions.forEach { db->
                copy.dbActions.add(db.copy() as DbAction)
            }
        }

        copy.isDeletable = isDeletable
        copy.shouldBefore.addAll(shouldBefore)
        copy.is2POST = is2POST

        return copy
    }

    /**
     * @return genes that represents this resource, i.e., longest action in this resource call
     */
    fun seeGenes(filter : GeneFilter = GeneFilter.NO_SQL) : List<out Gene>{
        return when(filter){
            GeneFilter.NO_SQL -> actions.flatMap(RestCallAction::seeGenes)
            GeneFilter.ONLY_SQL -> seeMutableSQLGenes()
            GeneFilter.ALL-> seeMutableSQLGenes().plus(actions.flatMap(RestCallAction::seeGenes))
            else -> throw IllegalArgumentException("there is no initialization in an ResourceCall")
        }
    }

    fun seeActions() : List<out Action> = dbActions.plus(actions)

    /**
     * @return the mutable SQL genes and they do not bind with any of Rest Actions
     *
     * */
    private fun seeMutableSQLGenes() : List<out Gene> = getResourceNode().getMutableSQLGenes(dbActions, getRestTemplate(), is2POST)

    fun repairGenesAfterMutation(mutatedGene: MutatedGeneSpecification?, cluster: ResourceCluster){

        mutatedGene?: log.warn("repair genes of resource call ({}) with null mutated genes", getResourceNode().getName())

        val boundBaseGenes = if (mutatedGene?.didStructureMutation() == true) longestPath().seeGenes() else mutatedGene?.getMutated(true)?.mapNotNull { it.gene } ?: longestPath().seeGenes()

        var anyMutated = false

        boundBaseGenes.filter(Gene::isMutable).map { g->
            val target = actions.find { it.seeGenes().contains(g) }
            if (target != null){
                anyMutated = true
                val param = (target as? RestCallAction)?.parameters?.find { it.seeGenes().contains(g) }
                    ?:throw IllegalStateException("${g.name} cannot be found in rest action ${target.getName()}")
                // bind actions with target
                actions.filter { it != target }
                    .forEach{a-> (a as RestCallAction).bindToSamePathResolution(target.path, listOf(param))}
            }
        }
        if (anyMutated && dbActions.isNotEmpty()){
            bindCallWithDbActions(dbActions,null, cluster, false, false)
        }
    }


    /**
     * bind [actions] in this call based on the given [dbActions] using a map [bindingMap] if there exists
     * @param dbActions are bound with [actions] in this call.
     * @param bindingMap is a map to bind [actions] and [dbActions] at gene-level, and it is nullable.
     *          if it is null, [SimpleDeriveResourceBinding] will be employed to derive the binding map based on the params of rest actions.
     * @param cluster records all existing resource node in the sut, here we need this because the [actions] might employ action from other resource node.
     * @param forceBindParamBasedOnDB specifies whether to bind params based on [dbActions] or reversed
     * @param dbRemovedDueToRepair indicates whether the dbactions are removed due to repair.
     */
    fun bindCallWithDbActions(dbActions: MutableList<DbAction>, bindingMap: Map<RestCallAction, MutableList<ParamGeneBindMap>>? = null,
                              cluster : ResourceCluster,
                              forceBindParamBasedOnDB: Boolean, dbRemovedDueToRepair : Boolean){
        var paramToTables = bindingMap
        if (paramToTables == null){
            val paramInfo = getResourceNode().getMissingParams(template!!.template, false)
            paramToTables = SimpleDeriveResourceBinding.generateRelatedTables(paramInfo, this, dbActions)
        }

        for (a in actions) {
            if (a is RestCallAction) {
                var list = paramToTables[a]
                if (list == null) list = paramToTables.filter { a.getName() == it.key.getName() }.values.run {
                    if (this.isEmpty()) null else this.first()
                }
                if (list != null && list.isNotEmpty()) {
                    BindingBuilder.bindRestAndDbAction(a, cluster.getResourceNode(a, true)!!, list, dbActions, forceBindParamBasedOnDB, dbRemovedDueToRepair)
                }
            }
        }
    }

    fun bindRestActionsWith(restResourceCalls: RestResourceCalls){
        if (restResourceCalls.getResourceNode().path != getResourceNode().path)
            throw IllegalArgumentException("target to bind refers to a different resource node, i.e., target (${restResourceCalls.getResourceNode().path}) vs. this (${getResourceNode().path})")
        val params = restResourceCalls.resourceInstance?.params?:restResourceCalls.actions.filterIsInstance<RestCallAction>().flatMap { it.parameters }
        actions.forEach { ac ->
            if((ac as RestCallAction).parameters.isNotEmpty()){
                ac.bindToSamePathResolution(ac.path, params)
            }
        }
    }


    /**
     * employing the longest action to represent a group of calls on a resource
     */
    private fun longestPath() : RestCallAction{
        val candidates = ParamUtil.selectLongestPathAction(actions)
        return candidates.first()
    }

    private fun repairGenePerAction(gene: Gene, action : RestCallAction){
        val genes = action.seeGenes().flatMap { g->g.flatView() }
        if(genes.contains(gene))
            genes.filter { ig-> ig != gene && ig.name == gene.name && ig::class.java.simpleName == gene::class.java.simpleName }.forEach {cg->
                cg.copyValueFrom(gene)
            }
    }

    fun extractTemplate() : String{
        return RestResourceTemplateHandler.getStringTemplateByCalls(this)
    }


    fun getRestTemplate() = template?.template?:RestResourceTemplateHandler.getStringTemplateByActions(actions as MutableList<RestCallAction>)

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