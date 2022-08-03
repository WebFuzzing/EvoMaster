package org.evomaster.core.problem.graphql

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.service.ApiWsIndividual
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class GraphQLIndividual(
        val sampleType: SampleType,
        allActions : MutableList<StructuralElement>
) : ApiWsIndividual(children = allActions) {


    constructor(actions: MutableList<GraphQLAction>,
                sampleType: SampleType,
                dbInitialization: MutableList<DbAction> = mutableListOf()
    ) : this(sampleType, dbInitialization.plus(actions).toMutableList())

    override fun copyContent(): Individual {

        return GraphQLIndividual(
                sampleType,
                children.map { it.copy() }.toMutableList()
        )

    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return when (filter) {
            GeneFilter.ALL -> seeInitializingActions().flatMap(Action::seeTopGenes).plus(seeActions().flatMap(Action::seeTopGenes))
            GeneFilter.NO_SQL -> seeActions().flatMap(Action::seeTopGenes)
            GeneFilter.ONLY_SQL -> seeDbActions().flatMap(DbAction::seeTopGenes)
            GeneFilter.ONLY_EXTERNAL_SERVICE -> seeInitializingActions().filterIsInstance<ExternalServiceAction>().flatMap(ExternalServiceAction::seeTopGenes)
        }
    }

    override fun size(): Int {
        return seeActions().size
    }

    override fun seeActions(): List<GraphQLAction> {
        return children.filterIsInstance<GraphQLAction>()
    }

    fun getIndexedCalls(): Map<Int,GraphQLAction> = getIndexedChildren(GraphQLAction::class.java)

    override fun seeActions(filter: ActionFilter): List<out Action> {
        // FIXME: This menthod requires refactoring, has duplicate under RPC and REST
        return when(filter){
            ActionFilter.ALL -> children as List<Action>
            ActionFilter.ONLY_SQL ->(children as List<Action>).filterIsInstance<DbAction>()
            ActionFilter.INIT -> seeInitializingActions()
            // TODO Man: need to systematically check NO_SQL that might be replaced with NO_INIT
            ActionFilter.NO_INIT, ActionFilter.NO_SQL -> seeActions()
            ActionFilter.ONLY_EXTERNAL_SERVICE -> (children as List<Action>).filterIsInstance<ExternalServiceAction>()
            ActionFilter.NO_EXTERNAL_SERVICE -> (children as List<Action>).filter { it !is ExternalServiceAction }
        }
    }

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(seeInitializingActions().filterIsInstance<DbAction>())
    }

    //TODO refactor to make sure all problem types use same/similar code with checks on indices

    fun addGQLAction(position: Int = -1, action: GraphQLAction){
        if (position == -1) addChild(action)
        else{
            addChild(position, action)
        }
    }

    fun removeGQLActionAt(position: Int){
        killChildByIndex(position)
    }

}