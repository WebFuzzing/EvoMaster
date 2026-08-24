package org.evomaster.core.problem.asyncapi.data

import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.database.sql.SqlAction
import kotlin.math.max

/**
 * A sequence of messages to publish, and whatever is needed to set the service up first.
 *
 * There is one individual for every transport rather than one per transport: Kafka versus AMQP
 * versus a socket appears nowhere in here. What is being searched over is the operation and the
 * payload, which are the same whatever moves the bytes; which wire is used is decided below the
 * driver interface. Were the transport to leak in here there would have to be a
 * `KafkaIndividual` and an `AmqpIndividual`, and nothing would be shared between them.
 *
 * Everything structural is inherited: initialization actions for seeding a database, a main
 * group of the messages under test, and cleanup.
 */
class AsyncApiIndividual(
    sampleType: SampleType,
    trackOperator: TrackOperator? = null,
    index: Int = -1,
    allActions: MutableList<ActionComponent>,
    mainSize: Int = allActions.size,
    sqlSize: Int = 0,
    mongoSize: Int = 0,
    redisSize: Int = 0,
    dnsSize: Int = 0,
    groups: GroupsOfChildren<StructuralElement> =
        getEnterpriseTopGroups(allActions, mainSize, sqlSize, mongoSize, redisSize, dnsSize, 0, 0)
) : ApiWsIndividual(
    sampleType,
    trackOperator,
    index,
    allActions,
    childTypeVerifier = EnterpriseChildTypeVerifier(AsyncApiAction::class.java),
    groups
) {

    constructor(
        sampleType: SampleType,
        actions: MutableList<AsyncApiAction>,
        dbInitialization: MutableList<SqlAction> = mutableListOf(),
        trackOperator: TrackOperator? = null,
        index: Int = -1
    ) : this(
        sampleType = sampleType,
        trackOperator = trackOperator,
        index = index,
        allActions = mutableListOf<ActionComponent>().apply {
            addAll(dbInitialization)
            addAll(actions.map { EnterpriseActionGroup(mutableListOf(it), AsyncApiAction::class.java) })
        },
        mainSize = actions.size,
        sqlSize = dbInitialization.size
    )

    override fun canMutateStructure(): Boolean = true

    /**
     * Add a message to publish, at [relativePosition] within the main group, or at the end.
     */
    fun addAction(relativePosition: Int = -1, action: AsyncApiAction) {

        val main = GroupsOfChildren.MAIN
        val group = EnterpriseActionGroup(mutableListOf(action), AsyncApiAction::class.java)

        if (relativePosition == -1) {
            addChildToGroup(group, main)
        } else {
            val base = groupsView()!!.startIndexForGroupInsertionInclusive(main)
            addChildToGroup(base + relativePosition, group, main)
        }
    }

    /**
     * Remove the message at [position] of the main group.
     */
    fun removeAction(position: Int) {
        killChildByIndex(firstIndexOfMainGroup() + position)
    }

    private fun firstIndexOfMainGroup() = max(
        0,
        max(
            children.indexOfLast { it is SqlAction } + 1,
            children.indexOfFirst { it is EnterpriseActionGroup<*> }
        )
    )

    /*
        Every group has to be measured, not just the two this class creates for itself. The
        children are copied wholesale, so a size left at its default would not match what is
        actually being handed over, and the group bookkeeping rejects that outright -- a copy
        would fail for an individual that had picked up, say, a Mongo insertion along the way.
     */
    override fun copyContent(): AsyncApiIndividual =
        AsyncApiIndividual(
            sampleType,
            trackOperator,
            index,
            children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
            mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN),
            sqlSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SQL),
            mongoSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_MONGO),
            redisSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_REDIS),
            dnsSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_DNS)
        )
}
