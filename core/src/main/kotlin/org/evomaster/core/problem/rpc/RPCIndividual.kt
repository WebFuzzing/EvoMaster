package org.evomaster.core.problem.rpc

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.service.ApiWsIndividual
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
        /**
         * actions of the individual
         */
        val actions: MutableList<RPCCallAction>,
        /*
            TODO might add sample type here as REST (check later)
         */
        dbInitialization: MutableList<DbAction> = mutableListOf(),
        trackOperator: TrackOperator? = null,
        index : Int = -1
) : ApiWsIndividual(dbInitialization, trackOperator, index, actions) {

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return when (filter) {
            GeneFilter.ALL -> seeInitializingActions().flatMap(DbAction::seeGenes).plus(seeActions().flatMap(Action::seeGenes))
            GeneFilter.NO_SQL -> seeActions().flatMap(Action::seeGenes)
            GeneFilter.ONLY_SQL -> seeInitializingActions().flatMap(DbAction::seeGenes)
        }
    }

    override fun size(): Int {
        return actions.size
    }

    override fun canMutateStructure(): Boolean = true

    override fun seeActions(filter: ActionFilter) : List<out Action>{
        return when(filter){
            ActionFilter.ALL -> seeInitializingActions().plus(actions)
            ActionFilter.NO_INIT, ActionFilter.NO_SQL -> seeActions()
            ActionFilter.ONLY_SQL, ActionFilter.INIT -> seeInitializingActions()
        }
    }

    override fun seeActions(): List<RPCCallAction> {
        return actions
    }

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(seeInitializingActions())
    }

    override fun getChildren(): List<StructuralElement> {
        return seeInitializingActions().plus(actions)
    }

    /**
     * add an action (ie, [action]) into [actions] at [position]
     */
    fun addAction(position: Int = -1, action: RPCCallAction){
        if (position == -1) actions.add(action)
        else{
            if (position > actions.size)
                throw IllegalStateException("specified position ($position) exceeds the range (${actions.size})")
            actions.add(position, action)
        }

        addChild(action)
    }

    /**
     * remove an action from [actions] at [position]
     */
    fun removeAction(position: Int){
        if (position >= actions.size)
            throw IllegalStateException("specified position ($position) exceeds the range (${actions.size})")
        val removed = actions.removeAt(position)
        removed.removeThisFromItsBindingGenes()
    }

    override fun copyContent(): Individual {
        return RPCIndividual(
            actions.map { it.copyContent() }.toMutableList(),
            seeInitializingActions().map { it.copyContent() as DbAction }.toMutableList(),
            trackOperator,
            index
        )
    }
}