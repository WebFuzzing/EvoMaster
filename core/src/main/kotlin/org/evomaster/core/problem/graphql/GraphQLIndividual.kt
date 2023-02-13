package org.evomaster.core.problem.graphql

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.Gene

class GraphQLIndividual(
        val sampleType: SampleType,
        allActions : MutableList<out ActionComponent>,
        mainSize : Int = allActions.size,
        dbSize: Int = 0,
        groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(allActions,mainSize,dbSize)
) : ApiWsIndividual(
    children = allActions,
    childTypeVerifier = {
        EnterpriseActionGroup::class.java.isAssignableFrom(it)
                || DbAction::class.java.isAssignableFrom(it)
    },
    groups = groups
) {


    override fun copyContent(): Individual {

        return GraphQLIndividual(
                sampleType,
                children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
                mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN),
                dbSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SQL)
        )

    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return when (filter) {
            GeneFilter.ALL -> seeAllActions().flatMap(Action::seeTopGenes)
            GeneFilter.NO_SQL -> seeActions(ActionFilter.NO_SQL).flatMap(Action::seeTopGenes)
            GeneFilter.ONLY_SQL -> seeDbActions().flatMap(DbAction::seeTopGenes)
            GeneFilter.ONLY_EXTERNAL_SERVICE -> seeExternalServiceActions().flatMap(ApiExternalServiceAction::seeTopGenes)
        }
    }


    fun addGQLAction(relativePosition: Int = -1, action: GraphQLAction){
        addMainActionInEmptyEnterpriseGroup(relativePosition, action)
    }

    fun removeGQLActionAt(relativePosition: Int){
        //FIXME isn't this wrong by ignoring initializing actions?
        killChildByIndex(relativePosition)
    }


    override fun seeMainExecutableActions() : List<GraphQLAction>{
        return super.seeMainExecutableActions() as List<GraphQLAction>
    }
}