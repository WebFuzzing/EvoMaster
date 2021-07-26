package org.evomaster.core.problem.rest.resource

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.util.RestResourceTemplateHandler
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.problem.util.inference.SimpleDeriveResourceBinding
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.Individual.GeneFilter
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * the class is used to structure actions regarding resources.
 * @property template is a resource template, e.g., POST-GET
 * @property node is a resource node which creates [this] call
 * @property actions is a sequence of actions in the [RestResourceCalls] that follows [template]
 * @property dbActions are used to initialize data for rest actions, either select from db or insert new data into db
 * @param withBinding specifies whether to build binding between rest genes
 *
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

    /**
     * represents the resource preparation status
     */
    var status = ResourceStatus.NOT_FOUND

    /**
     * whether the call can be deleted during structure mutation
     */
    var isDeletable = true

    /**
     * this call should be before [shouldBefore]
     */
    val shouldBefore = mutableListOf<String>()

    /**
     *  the dependency is built between [this] and [depends] in this individual
     *  the dependency here means that there exist binding genes among the calls.
     */
    val depends = mutableSetOf<String>()

    final override fun copy(): RestResourceCalls {
        val copy = super.copy()
        if (copy !is RestResourceCalls)
            throw IllegalStateException("mismatched type: the type should be RestResourceCall, but it is ${this::class.java.simpleName}")
        return copy
    }

    /**
     * @return children of RestResourceCall which is a sequence of [dbActions] and [actions]
     */
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
        copy.depends.addAll(depends)

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

    /**
     * @return actions with specified action [filter]
     */
    fun seeActions(filter: ActionFilter) : List<out Action>{
        return when(filter){
            ActionFilter.ALL-> dbActions.plus(actions)
            ActionFilter.INIT, ActionFilter.ONLY_SQL -> dbActions
            ActionFilter.NO_INIT,
            ActionFilter.NO_SQL -> actions
        }
    }

    /**
     * @return size of action with specified action [filter]
     */
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

    /**
     * reset dbactions with [actions]
     */
    fun resetDbAction(actions: List<DbAction>){
        dbActions.clear()
        dbActions.addAll(actions)
        addChildren(actions)

        (getRoot() as? RestIndividual)?.cleanBrokenBindingReference()
    }

    /**
     * remove dbaction based on [removePredict]
     */
    fun removeDbActionIf(removePredict: (DbAction) -> Boolean){
        val removed = dbActions.filter {removePredict(it)}
        resetDbAction(removed)
    }

    private fun removeDbActions(remove: List<DbAction>){
        val removedGenes = remove.flatMap { it.seeGenes() }.flatMap { it.flatView() }
        dbActions.removeAll(remove)
        (dbActions.plus(actions).flatMap { it.seeGenes() }).flatMap { it.flatView() }.filter { it.isBoundGene() }.forEach {
            it.cleanRemovedGenes(removedGenes)
        }
    }

    /**
     * @return the mutable SQL genes and they do not bind with any of Rest Actions
     *
     * */
    private fun seeMutableSQLGenes() : List<out Gene> = getResourceNode().getMutableSQLGenes(dbActions, getRestTemplate(), is2POST)


    /**
     * bind this with other [relatedResourceCalls]
     * @param relatedResourceCalls to be bound with [this]
     * @param doRemoveDuplicatedTable specifies whether to remove duplicated db actions on this
     *      e.g., for resource C, table C is created, in addition, A and B are also created since B refers to them,
     *      in this case, if the following handling is related to A and B, we do not further create A and B once [doRemoveDuplicatedTable] is true
     */
    fun bindWithOtherRestResourceCalls(relatedResourceCalls: MutableList<RestResourceCalls>, cluster: ResourceCluster, doRemoveDuplicatedTable: Boolean){
        // handling [this.dbActions]
        if (this.dbActions.isNotEmpty() && doRemoveDuplicatedTable){
            removeDuplicatedDbActions(relatedResourceCalls, cluster, doRemoveDuplicatedTable)
        }

        // bind with rest actions
        actions.forEach { current->
            relatedResourceCalls.forEach { call->
                call.seeActions(ActionFilter.NO_SQL).forEach { previous->
                    if (previous is RestCallAction){
                        val dependent = current.bindBasedOn(previous)
                        if (dependent){
                            setDependentCall(call)
                        }

                    }
                }
            }
        }

        // synchronize values based on rest actions
        syncValues(true)
    }


    /*
        verify the binding which is only useful for debugging
     */
    fun verifyBindingGenes(other : List<RestResourceCalls>): Boolean{
        val currentAll = seeActions(ActionFilter.ALL).flatMap { it.seeGenes() }.flatMap { it.flatView() }
        val otherAll = other.flatMap { it.seeActions(ActionFilter.ALL) }.flatMap { it.seeGenes() }.flatMap { it.flatView() }

        currentAll.forEach { g->
            val root = g.getRoot()
            val ok = root is RestResourceCalls || root is RestIndividual
            if (!ok)
                return false

            if (g.isBoundGene()){
                val inside = g.bindingGeneIsSubsetOf(currentAll.plus(otherAll))
                if (!inside)
                    return false
            }
        }

        return true
    }

    fun repairFK(previous: List<DbAction>){

        if (!DbActionUtils.verifyForeignKeys(previous.plus(dbActions))){
            val current = previous.toMutableList()
            dbActions.forEach { d->
                val ok = DbActionUtils.repairFk(d, current)
                if (!ok.first){
                    throw IllegalStateException("fail to find pk in the previous dbactions")
                }
                current.add(d)
            }

            Lazy.assert { DbActionUtils.verifyForeignKeys(previous.plus(dbActions)) }
        }
    }

    /**
     * synchronize values in this call
     * @param withRest specifies whether to synchronize values based on rest actions ([withRest] is true) or db actions ([withRest] is false)
     */
    private fun syncValues(withRest: Boolean = true){
        (if (withRest) actions else dbActions).forEach {
            it.seeGenes().flatMap { i-> i.flatView() }.forEach { g->
                g.syncBindingGenesBasedOnThis()
            }
        }
    }

    private fun removeDuplicatedDbActions(calls: List<RestResourceCalls>, cluster: ResourceCluster, doRemoveDuplicatedTable: Boolean){

        val dbRelatedToTables = calls.flatMap {  it.seeActions(ActionFilter.ONLY_SQL) as List<DbAction> }.map { it.table.name }.toHashSet()

        val dbactionInOtherCalls = calls.flatMap {  it.seeActions(ActionFilter.ONLY_SQL) as List<DbAction> }
        // remove duplicated dbactions
        if (doRemoveDuplicatedTable){
            val dbActionsToRemove = this.dbActions.filter { dbRelatedToTables.contains(it.table.name) }
            if (dbActionsToRemove.isNotEmpty()){
                removeDbActions(dbActionsToRemove)
                val frontDbActions = dbactionInOtherCalls.toMutableList()
                this.dbActions
                    .forEach {db->
                        // fix fk with front dbactions
                        val ok = DbActionUtils.repairFk(db, frontDbActions)
                        if (!ok.first){
                            throw IllegalStateException("cannot fix the fk of ${db.getResolvedName()}")
                        }
                        ok.second?.forEach { db->
                            val call = calls.find { it.seeActions(ActionFilter.ONLY_SQL).contains(db) }!!
                            setDependentCall(call)
                            // handling rest action binding with the fixed db which is in a different call
                            if (dbactionInOtherCalls.contains(db)){
                                bindRestActionBasedOnDbActions(listOf(db), cluster, false, false)
                            }
                        }
                        frontDbActions.add(db)
                    }
            }
        }
    }

    private fun setDependentCall(calls: RestResourceCalls){
        calls.isDeletable = false
        calls.shouldBefore.add(getResourceNodeKey())
        depends.add(getResourceNodeKey())
    }


    /**
     *  init dbactions for [this] RestResourceCall which would build binding relationship with its rest [actions].
     *  @param dbActions specified the dbactions to be initialized for this call
     *  @param cluster specified the resource cluster
     *  @param forceBindParamBasedOnDB specified whether to force to bind values of params in rest actions based on dbactions
     *  @param dbRemovedDueToRepair specified whether any db action is removed due to repair process.
     *          Note that dbactions might be truncated in the db repair process, thus the table related to rest actions might be removed.
     *  @param bindWith specified a list of resource call which might be bound with [this]
     */
    fun initDbActions(dbActions: List<DbAction>, cluster: ResourceCluster, forceBindParamBasedOnDB: Boolean, dbRemovedDueToRepair : Boolean, bindWith: List<RestResourceCalls>? = null){
        bindWith?.forEach { p->
            val dependent = p.seeActions(ActionFilter.ONLY_SQL).any { dbActions.contains(it) }
            if (dependent){
                setDependentCall(p)
            }
        }

        if (this.dbActions.isNotEmpty()) throw IllegalStateException("dbactions of this RestResourceCall is not empty")
        this.dbActions.addAll(dbActions)
        addChildren(dbActions)

        bindRestActionBasedOnDbActions(dbActions, cluster, forceBindParamBasedOnDB, dbRemovedDueToRepair)

    }

    private fun bindRestActionBasedOnDbActions(dbActions: List<DbAction>, cluster: ResourceCluster, forceBindParamBasedOnDB: Boolean, dbRemovedDueToRepair : Boolean){

        val paramInfo = getResourceNode().getPossiblyBoundParams(template!!.template, is2POST)
        val paramToTables = SimpleDeriveResourceBinding.generateRelatedTables(paramInfo, this, dbActions)

        if (paramToTables.isEmpty()) return

        for (restaction in actions) {
            var list = paramToTables[restaction]
            if (list == null) list = paramToTables.filter { restaction.getName() == it.key.getName() }.values.run {
                if (this.isEmpty()) null else this.first()
            }
            if (list != null && list.isNotEmpty()) {
                BindingBuilder.bindRestAndDbAction(restaction, cluster.getResourceNode(restaction, true)!!, list, dbActions, forceBindParamBasedOnDB, dbRemovedDueToRepair, true)
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

    fun removeThisFromItsBindingGenes(){
        (dbActions.plus(actions)).forEach { a->
            a.seeGenes().forEach { g->
                g.flatView().forEach { r->
                    r.removeThisFromItsBindingGenes()
                }
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