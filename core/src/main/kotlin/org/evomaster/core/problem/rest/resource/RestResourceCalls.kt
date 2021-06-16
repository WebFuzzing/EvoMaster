package org.evomaster.core.problem.rest.resource

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.util.RestResourceTemplateHandler
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.problem.util.inference.SimpleDeriveResourceBinding
import org.evomaster.core.problem.util.inference.model.ParamGeneBindMap
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.Individual.GeneFilter
import org.evomaster.core.search.StructuralElement
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
 *
 * TODO remove [resourceInstance]
 */
class RestResourceCalls(
    val template: CallsTemplate? = null,
    val node: RestResourceNode? = null,
    private val actions: MutableList<RestCallAction>,
    private val dbActions: MutableList<DbAction> = mutableListOf(),
    withBinding: Boolean = false
): StructuralElement(mutableListOf<StructuralElement>().apply { addAll(dbActions); addAll(actions) }){

    companion object{
        private val  log : Logger = LoggerFactory.getLogger(RestResourceCalls::class.java)
    }

    init {
        if (withBinding)
            buildBindingGene()
    }

    /**
     * build gene binding among rest actions, ie, [actions]
     * e.g., a sequence of actions
     *     0, POST /A
     *     1, POST /A/{a}
     *     2, POST /A/{a}/B
     *     3, GET /A/{a}/B/{b}
     * (0-2) actions bind values based on the action at 3
     */
    private fun buildBindingGene(){
        if (actions.size == 1) return
        (0 until actions.size-1).forEach {
            actions[it].bindBasedOn(actions.last())
        }

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


    final override fun copy(): RestResourceCalls {
        val copy = super.copy()
        if (copy !is RestResourceCalls)
            throw IllegalStateException("mismatched type: the type should be RestResourceCall, but it is ${this::class.java.simpleName}")
        return copy
    }

    override fun getChildren(): List<Action> = dbActions.plus(actions)

    override fun copyContent() : RestResourceCalls{
        val copy = RestResourceCalls(
            template,
            node,
            actions.map { a -> a.copyContent() as RestCallAction}.toMutableList(),
            dbActions.map { db-> db.copyContent() as DbAction }.toMutableList(),
            withBinding = false
        )

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

    fun seeActions(filter: ActionFilter) : List<out Action>{
        return when(filter){
            ActionFilter.ALL-> dbActions.plus(actions)
            ActionFilter.INIT, ActionFilter.ONLY_SQL -> dbActions
            ActionFilter.NO_INIT,
            ActionFilter.NO_SQL -> actions
        }
    }

    fun seeActionSize(filter: ActionFilter) : Int{
        return seeActions(filter).size
    }

    fun addDbAction(position : Int = -1, actions: List<DbAction>){
        if (position == -1) dbActions.addAll(actions)
        else{
            dbActions.addAll(position, actions)
        }
        addChildren(actions)
    }

    fun resetDbAction(actions: List<DbAction>){
        dbActions.clear()
        dbActions.addAll(actions)
        addChildren(actions)
    }

    fun removeDbActionIf(removePredict: (DbAction) -> Boolean){
        dbActions.removeIf{
            removePredict(it)
        }
    }


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
                    .forEach{a-> a.bindBasedOn(target.path, listOf(param))}
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
            val paramInfo = getResourceNode().getPossiblyBoundParams(template!!.template, false)
            paramToTables = SimpleDeriveResourceBinding.generateRelatedTables(paramInfo, this, dbActions)
        }

        for (a in actions) {
            var list = paramToTables[a]
            if (list == null) list = paramToTables.filter { a.getName() == it.key.getName() }.values.run {
                if (this.isEmpty()) null else this.first()
            }
            if (list != null && list.isNotEmpty()) {
                BindingBuilder.bindRestAndDbAction(a, cluster.getResourceNode(a, true)!!, list, dbActions, forceBindParamBasedOnDB, dbRemovedDueToRepair)
            }
        }
    }


    /**
     * build the binding between [this] with other [restResourceCalls]
     */
    fun bindRestActionsWith(restResourceCalls: RestResourceCalls){
        if (restResourceCalls.getResourceNode().path != getResourceNode().path)
            throw IllegalArgumentException("target to bind refers to a different resource node, i.e., target (${restResourceCalls.getResourceNode().path}) vs. this (${getResourceNode().path})")
        val params = restResourceCalls.actions.flatMap { it.parameters }
        actions.forEach { ac ->
            if(ac.parameters.isNotEmpty()){
                ac.bindBasedOn(ac.path, params)
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

    private fun getParamsInCall() : List<Param>  = actions.flatMap { it.parameters }

    fun getResolvedKey() : String{
        return node?.path?.resolve(getParamsInCall())?: throw IllegalStateException("node is null")
    }

    fun getAResourceKey() : String = node?.path.toString()?: throw IllegalStateException("node is null")

    fun getRestTemplate() = template?.template?: RestResourceTemplateHandler.getStringTemplateByActions(actions as MutableList<RestCallAction>)

    fun getResourceNode() : RestResourceNode = node?:throw IllegalArgumentException("the individual does not have resource structure")

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
    NOT_NEEDED(1),
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