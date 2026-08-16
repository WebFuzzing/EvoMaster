package org.evomaster.core.problem.rpc

import org.evomaster.core.Lazy
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.scheduletask.ScheduleTaskAction

import org.evomaster.core.search.*
import org.evomaster.core.search.tracer.TrackOperator
import java.util.*
import kotlin.math.max

/**
 * individual for RPC service
 */
class RPCIndividual(
    sampleType: SampleType,
    trackOperator: TrackOperator? = null,
    index: Int = -1,
    allActions: MutableList<ActionComponent>,
    mainSize: Int = allActions.size,
    sqlSize: Int = 0,
    mongoSize: Int = 0,
    redisSize: Int = 0,
    dnsSize: Int = 0,
    scheduleTaskSize : Int = 0,
    groups: GroupsOfChildren<StructuralElement> =
        getEnterpriseTopGroups(allActions, mainSize, sqlSize,mongoSize,redisSize,dnsSize, scheduleTaskSize, 0)
) : ApiWsIndividual(
    sampleType,
    trackOperator, index, allActions,
    childTypeVerifier = EnterpriseChildTypeVerifier(RPCCallAction::class.java),
    groups
) {

    constructor(
        sampleType: SampleType,
        actions: MutableList<RPCCallAction>,
        externalServicesActions: MutableList<List<ApiExternalServiceAction>> = mutableListOf(),
        scheduleTaskActions: MutableList<ScheduleTaskAction> = mutableListOf(),
        /*
            TODO might add sample type here as REST (check later)
         */
        dbInitialization: MutableList<SqlAction> = mutableListOf(),
        trackOperator: TrackOperator? = null,
        index: Int = -1
    ) : this(
        sampleType = sampleType,
        trackOperator = trackOperator,
        index = index,
        allActions = mutableListOf<ActionComponent>().apply {
            addAll(dbInitialization)
            addAll(scheduleTaskActions)
            addAll(actions.mapIndexed { index, rpcCallAction ->
                if (externalServicesActions.isNotEmpty()){
                    Lazy.assert { actions.size == externalServicesActions.size }
                }
                EnterpriseActionGroup(mutableListOf(rpcCallAction), RPCCallAction::class.java).apply {
                    /*
                        externalServicesActions defaults to empty, meaning no call has any
                        mocked external service. Indexing into it regardless would throw for
                        every call, making the default unusable.
                     */
                    if (index < externalServicesActions.size) {
                        addChildrenToGroup(
                            externalServicesActions[index],
                            GroupsOfChildren.EXTERNAL_SERVICES
                        )
                    }
                }})
        },
        mainSize = actions.size,
        sqlSize = dbInitialization.size,
        scheduleTaskSize = scheduleTaskActions.size
        )

    override fun canMutateStructure(): Boolean = true


    /**
     * The RPC calls of this individual, by the index of the child holding each.
     *
     * Note the calls are not children of the individual directly: each is wrapped in an
     * [EnterpriseActionGroup], alongside the external-service actions that belong to it. So
     * asking for children of type [RPCCallAction] finds nothing, and the group has to be
     * unwrapped.
     */
    fun seeIndexedRPCCalls(): Map<Int, RPCCallAction> =
        getIndexedChildren(EnterpriseActionGroup::class.java)
            .mapNotNull { (index, group) ->
                (group.getMainAction() as? RPCCallAction)?.let { index to it }
            }
            .toMap()


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
        killChildByIndex(getFirstIndexOfEnterpriseActionGroup() + position) as EnterpriseActionGroup<*>
    }

    private fun getFirstIndexOfEnterpriseActionGroup() = max(0, max(children.indexOfLast { it is SqlAction }+1, children.indexOfFirst { it is EnterpriseActionGroup<*> }))

    override fun copyContent(): Individual {
        return RPCIndividual(
            sampleType,
            trackOperator,
            index,
            children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
            mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN),
            sqlSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SQL),
            mongoSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_MONGO),
            redisSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_REDIS),
            dnsSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_DNS),
            scheduleTaskSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SCHEDULE_TASK)
        )
    }

    override fun seeMainExecutableActions(): List<RPCCallAction> {
        return super.seeMainExecutableActions() as List<RPCCallAction>
    }


    /**
     * @return a sorted list of involved interfaces in this test
     */
    fun getTestedInterfaces() : SortedSet<String> {
        return seeMainExecutableActions().map { it.interfaceId }.toSortedSet()
    }
}
