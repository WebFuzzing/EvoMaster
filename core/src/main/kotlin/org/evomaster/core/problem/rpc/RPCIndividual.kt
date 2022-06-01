package org.evomaster.core.problem.rpc

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.service.ApiWsIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.tracer.TrackOperator

/**
 * individual for RPC service
 */
class RPCIndividual(
        trackOperator: TrackOperator? = null,
        index: Int = -1,
        allActions: MutableList<StructuralElement>
) : ApiWsIndividual(trackOperator, index, allActions) {

    constructor(actions: MutableList<RPCCallAction>,
            /*
                TODO might add sample type here as REST (check later)
             */
                dbInitialization: MutableList<DbAction> = mutableListOf(),
                trackOperator: TrackOperator? = null,
                index: Int = -1
    ) : this(trackOperator, index, mutableListOf<StructuralElement>().apply {
        addAll(dbInitialization); addAll(actions)
    })


    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return when (filter) {
            GeneFilter.ALL -> seeInitializingActions().flatMap(DbAction::seeGenes).plus(seeActions().flatMap(Action::seeGenes))
            GeneFilter.NO_SQL -> seeActions().flatMap(Action::seeGenes)
            GeneFilter.ONLY_SQL -> seeInitializingActions().flatMap(DbAction::seeGenes)
        }
    }

    override fun size(): Int {
        return seeActions().size
    }

    override fun canMutateStructure(): Boolean = true

    override fun seeActions(filter: ActionFilter): List<out Action> {
        return when (filter) {
            ActionFilter.ALL -> children as List<Action>
            ActionFilter.NO_INIT, ActionFilter.NO_SQL -> seeActions()
            ActionFilter.ONLY_SQL, ActionFilter.INIT -> seeInitializingActions()
        }
    }

    override fun seeActions(): List<RPCCallAction> {
        return children.filterIsInstance<RPCCallAction>()
    }

     fun seeIndexedRPCCalls() : Map<Int, RPCCallAction>{
        val m  = mutableMapOf<Int, RPCCallAction>()
        for(i in children.indices){
            val child = children[i]
            if(child !is RPCCallAction){
                continue
            }
            m[i] = child
        }
        return m
    }

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(seeInitializingActions())
    }


    /**
     * add an action (ie, [action]) into [actions] at [position]
     */
    fun addAction(position: Int = -1, action: RPCCallAction) {
        if (position == -1) addChild(action)
        else {
            if (position > children.size)
                throw IllegalStateException("specified position ($position) exceeds the range (${children.size})")
            addChild(position, action)
        }
    }

    /**
     * remove an action from [actions] at [position]
     */
    fun removeAction(position: Int) {
        if (!seeIndexedRPCCalls().keys.contains(position))
            throw IllegalStateException("specified position ($position) is not in range")
        val removed = killChildByIndex(position) as RPCCallAction
        removed.removeThisFromItsBindingGenes()
    }

    override fun copyContent(): Individual {
        return RPCIndividual(
                trackOperator,
                index,
                children.map { it.copy() }.toMutableList()
        )
    }
}