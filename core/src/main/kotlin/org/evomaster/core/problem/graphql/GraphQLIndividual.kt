package org.evomaster.core.problem.graphql

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.httpws.service.HttpWsIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.tracer.TraceableElementCopyFilter

class GraphQLIndividual(
        val actions: MutableList<GraphQLAction>,
        val sampleType: SampleType,
        dbInitialization: MutableList<DbAction> = mutableListOf()
) : HttpWsIndividual(dbInitialization= dbInitialization, children = dbInitialization.plus(actions)) {

    override fun copyContent(): Individual {

        return GraphQLIndividual(
                actions.map { it.copyContent() as GraphQLAction}.toMutableList(),
                sampleType,
                seeInitializingActions().map { it.copyContent() as DbAction } as MutableList<DbAction>
        )

    }

    override fun getChildren(): List<Action> = seeInitializingActions().plus(actions)

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
        return actions

    }

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(seeInitializingActions())
    }


    override fun copy(copyFilter: TraceableElementCopyFilter): GraphQLIndividual {
        val copy = copy() as GraphQLIndividual
        when(copyFilter){
            TraceableElementCopyFilter.NONE-> {}
            TraceableElementCopyFilter.WITH_TRACK, TraceableElementCopyFilter.DEEP_TRACK  ->{
                copy.wrapWithTracking(null, tracking!!.copy())
            }else -> throw IllegalStateException("${copyFilter.name} is not implemented!")
        }
        return copy
    }

}