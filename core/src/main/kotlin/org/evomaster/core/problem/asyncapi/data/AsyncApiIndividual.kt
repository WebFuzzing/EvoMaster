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
 * A test case for an AsyncAPI service: the messages to publish, in order, after whatever
 * setup the service needs, such as rows in a database.
 *
 * Which transport carries the messages is the driver's concern, so one individual serves
 * them all.
 */
class AsyncApiIndividual(
    sampleType: SampleType,
    trackOperator: TrackOperator? = null,
    index: Int = -1,
    allActions: MutableList<ActionComponent>,
    mainSize: Int = allActions.size,
    /*
        TODO Only SQL, Mongo and DNS are handled here for now. EvoMaster is gaining support for
        several more databases; when one of them is fully supported for REST, it gets a size
        parameter here and in copyContent(), and a slot in the getEnterpriseTopGroups call.
     */
    sqlSize: Int = 0,
    mongoSize: Int = 0,
    dnsSize: Int = 0,
    /*
        No Redis group. Every Redis option in EMConfig is still @Experimental, so a new problem
        type should not inherit that seam; the size is fixed at zero until Redis is fully
        supported for REST, at which point this is one parameter to add.
     */
    groups: GroupsOfChildren<StructuralElement> =
        getEnterpriseTopGroups(allActions, mainSize, sqlSize, mongoSize, 0, dnsSize, 0, 0)
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
     *
     * @throws IllegalArgumentException if [relativePosition] is neither -1 nor a position in
     *                                  the main group, one past its last message included
     */
    fun addAction(relativePosition: Int = -1, action: AsyncApiAction) {

        val main = GroupsOfChildren.MAIN
        val size = groupsView()!!.sizeOfGroup(main)

        if (relativePosition < -1 || relativePosition > size) {
            throw IllegalArgumentException(
                "Position $relativePosition is out of range: the individual holds $size messages"
            )
        }

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
     *
     * @throws IllegalArgumentException if there is no message at [position]
     */
    fun removeAction(position: Int) {

        val size = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN)

        if (position < 0 || position >= size) {
            throw IllegalArgumentException(
                "Position $position is out of range: the individual holds $size messages"
            )
        }

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
        Every group is measured, not just the ones this class creates: the children are copied
        wholesale, and a size left at its default would not match them.
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
            dnsSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_DNS)
        )
}
