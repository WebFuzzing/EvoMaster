package org.evomaster.core.problem.graphql

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.service.ApiWsIndividual
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.Gene

class GraphQLIndividual(
        val sampleType: SampleType,
        allActions : MutableList<EnterpriseActionGroup>
) : ApiWsIndividual(children = allActions) {


    override fun copyContent(): Individual {

        return GraphQLIndividual(
                sampleType,
                children.map { it.copy() }.toMutableList() as MutableList<EnterpriseActionGroup>
        )

    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return when (filter) {
            GeneFilter.ALL -> seeInitializingActions().flatMap(Action::seeTopGenes).plus(seeAllActions().flatMap(Action::seeTopGenes))
            GeneFilter.NO_SQL -> seeAllActions().flatMap(Action::seeTopGenes)
            GeneFilter.ONLY_SQL -> seeDbActions().flatMap(DbAction::seeTopGenes)
            GeneFilter.ONLY_EXTERNAL_SERVICE -> seeInitializingActions().filterIsInstance<ExternalServiceAction>().flatMap(ExternalServiceAction::seeTopGenes)
        }
    }

    override fun size(): Int {
        return seeAllActions().size
    }

    fun getIndexedCalls(): Map<Int,GraphQLAction> = getIndexedChildren(GraphQLAction::class.java)


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