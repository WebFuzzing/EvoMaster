package org.evomaster.core.problem.rpc

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction

import org.evomaster.core.search.*
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.tracer.TrackOperator
import kotlin.math.max

/**
 * individual for RPC service
 */
class RPCIndividual(
    trackOperator: TrackOperator? = null,
    index: Int = -1,
    allActions: MutableList<ActionComponent>,
    mainSize: Int = allActions.size,
    dbSize: Int = 0,
    groups: GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(allActions, mainSize, dbSize)
) : ApiWsIndividual(
    trackOperator, index, allActions,
    childTypeVerifier = {
        EnterpriseActionGroup::class.java.isAssignableFrom(it)
                || DbAction::class.java.isAssignableFrom(it)
    },
    groups
) {

    constructor(
        actions: MutableList<RPCCallAction>,
        externalServicesActions: MutableList<List<ApiExternalServiceAction>> = mutableListOf(),
        /*
            TODO might add sample type here as REST (check later)
         */
        dbInitialization: MutableList<DbAction> = mutableListOf(),
        trackOperator: TrackOperator? = null,
        index: Int = -1
    ) : this(
        trackOperator = trackOperator,
        index = index,
        allActions = mutableListOf<ActionComponent>().apply {
            addAll(dbInitialization);
            addAll(actions.mapIndexed { index, rpcCallAction ->
                if (externalServicesActions.isNotEmpty())
                    Lazy.assert { actions.size == externalServicesActions.size }
                EnterpriseActionGroup(mutableListOf(rpcCallAction), RPCCallAction::class.java).apply {
                    addChildrenToGroup(
                        externalServicesActions[index],
                        GroupsOfChildren.EXTERNAL_SERVICES
                    )
                }})
        },
        mainSize = actions.size, dbSize = dbInitialization.size)

    /**
     * TODO: Verify the implementation
     */
    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return when (filter) {
            GeneFilter.ALL -> seeAllActions().flatMap(Action::seeTopGenes)
            GeneFilter.NO_SQL -> seeActions(ActionFilter.NO_SQL).flatMap(Action::seeTopGenes)
            GeneFilter.ONLY_SQL -> seeDbActions().flatMap(DbAction::seeTopGenes)
            GeneFilter.ONLY_EXTERNAL_SERVICE -> seeExternalServiceActions().flatMap(ApiExternalServiceAction::seeTopGenes)
        }
    }

    override fun size(): Int {
        return seeMainExecutableActions().size
    }

    override fun canMutateStructure(): Boolean = true


    fun seeIndexedRPCCalls(): Map<Int, RPCCallAction> = getIndexedChildren(RPCCallAction::class.java)

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(seeInitializingActions().filterIsInstance<DbAction>())
    }


    /**
     * add an action (ie, [action]) into [actions] at [position]
     */
    fun addAction(relativePosition: Int = -1, action: RPCCallAction) {
        val main = GroupsOfChildren.MAIN
        val g = EnterpriseActionGroup(mutableListOf(action), RPCCallAction::class.java)

        if (relativePosition == -1) {
            addChildToGroup(g, main)
        } else{
            val base = groupsView()!!.startIndexForGroupInsertionInclusive(main)
            val position = base + relativePosition
            addChildToGroup(position, action, main)
        }
    }

    /**
     * remove an action from [actions] at [position]
     */
    fun removeAction(position: Int) {
        val removed = (killChildByIndex(getFirstIndexOfEnterpriseActionGroup() + position) as EnterpriseActionGroup).getMainAction()
        removed.removeThisFromItsBindingGenes()
    }

    private fun getFirstIndexOfEnterpriseActionGroup() = max(0, max(children.indexOfLast { it is DbAction }+1, children.indexOfFirst { it is EnterpriseActionGroup }))

    override fun copyContent(): Individual {
        return RPCIndividual(
            trackOperator,
            index,
            children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
            mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN),
            dbSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SQL)
        )
    }

    override fun seeMainExecutableActions(): List<RPCCallAction> {
        return super.seeMainExecutableActions() as List<RPCCallAction>
    }
}