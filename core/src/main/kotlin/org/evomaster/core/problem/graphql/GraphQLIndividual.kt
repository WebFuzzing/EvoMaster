package org.evomaster.core.problem.graphql

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.service.ApiWsIndividual
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.tracer.TraceableElementCopyFilter

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
            GeneFilter.ALL -> seeInitializingActions().flatMap(DbAction::seeGenes).plus(seeActions().flatMap(Action::seeGenes))
            GeneFilter.NO_SQL -> seeActions().flatMap(Action::seeGenes)
            GeneFilter.ONLY_SQL -> seeInitializingActions().flatMap(DbAction::seeGenes)
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
        return when(filter){
            ActionFilter.ALL -> children as List<Action>
            ActionFilter.ONLY_SQL, ActionFilter.INIT -> seeInitializingActions()
            ActionFilter.NO_INIT, ActionFilter.NO_SQL -> seeActions()
        }
    }

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(seeInitializingActions())
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