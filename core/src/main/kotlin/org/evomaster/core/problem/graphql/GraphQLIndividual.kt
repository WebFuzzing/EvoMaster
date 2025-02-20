package org.evomaster.core.problem.graphql

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.Gene

class GraphQLIndividual(
    sampleType: SampleType,
    allActions : MutableList<out ActionComponent>,
    mainSize : Int = allActions.size,
    sqlSize: Int = 0,
    mongoSize: Int = 0,
    dnsSize: Int = 0,
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(allActions,mainSize,sqlSize,mongoSize,dnsSize,0)
) : ApiWsIndividual(
    sampleType = sampleType,
    children = allActions,
    childTypeVerifier = EnterpriseChildTypeVerifier(GraphQLAction::class.java),
    groups = groups
) {


    override fun copyContent(): Individual {

        return GraphQLIndividual(
                sampleType,
                children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
                mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN),
                sqlSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SQL),
                mongoSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_MONGO),
                dnsSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_DNS)
        )

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
