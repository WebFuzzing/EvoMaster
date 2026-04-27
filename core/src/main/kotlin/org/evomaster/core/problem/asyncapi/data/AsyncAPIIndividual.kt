package org.evomaster.core.problem.asyncapi.data

import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.tracer.TrackOperator
import kotlin.math.max

/**
 * Test case targeting an AsyncAPI 3.0 SUT.
 *
 * Each main-group action is an [AsyncAPIAction] wrapped in an
 * [EnterpriseActionGroup], mirroring the RPC layout.  PUBLISH and
 * SUBSCRIBE_REPLY actions referring to the same `pairId` must stay adjacent;
 * the structure mutator (M4) enforces this invariant when it adds or removes
 * actions.
 */
class AsyncAPIIndividual(
    sampleType: SampleType,
    trackOperator: TrackOperator? = null,
    index: Int = -1,
    allActions: MutableList<ActionComponent>,
    mainSize: Int = allActions.size,
    groups: GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(
        allActions, mainSize, 0, 0, 0, 0, 0
    )
) : ApiWsIndividual(
    sampleType,
    trackOperator,
    index,
    allActions,
    childTypeVerifier = EnterpriseChildTypeVerifier(AsyncAPIAction::class.java),
    groups
) {

    constructor(
        sampleType: SampleType,
        actions: MutableList<AsyncAPIAction>,
        trackOperator: TrackOperator? = null,
        index: Int = -1
    ) : this(
        sampleType = sampleType,
        trackOperator = trackOperator,
        index = index,
        allActions = actions
            .map { EnterpriseActionGroup(mutableListOf(it), AsyncAPIAction::class.java) }
            .toMutableList<ActionComponent>(),
        mainSize = actions.size
    )

    override fun canMutateStructure(): Boolean = true

    override fun copyContent(): Individual {
        return AsyncAPIIndividual(
            sampleType,
            trackOperator,
            index,
            children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
            mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN)
        )
    }

    override fun seeMainExecutableActions(): List<AsyncAPIAction> {
        @Suppress("UNCHECKED_CAST")
        return super.seeMainExecutableActions() as List<AsyncAPIAction>
    }

    /**
     * Append [action] to the MAIN group.  Intended for the sampler; the
     * structure mutator should use [insertAction] for finer-grained control.
     */
    fun addAction(action: AsyncAPIAction) {
        val group = EnterpriseActionGroup(mutableListOf(action), AsyncAPIAction::class.java)
        addChildToGroup(group, GroupsOfChildren.MAIN)
    }

    /**
     * Insert [action] at [relativePosition] within the MAIN group, where 0 is
     * the first MAIN action.
     */
    fun insertAction(relativePosition: Int, action: AsyncAPIAction) {
        val group = EnterpriseActionGroup(mutableListOf(action), AsyncAPIAction::class.java)
        if (relativePosition < 0) {
            addChildToGroup(group, GroupsOfChildren.MAIN)
        } else {
            val base = groupsView()!!.startIndexForGroupInsertionInclusive(GroupsOfChildren.MAIN)
            addChildToGroup(base + relativePosition, group, GroupsOfChildren.MAIN)
        }
    }

    fun removeAction(relativePosition: Int) {
        killChildByIndex(firstMainGroupIndex() + relativePosition) as EnterpriseActionGroup<*>
    }

    private fun firstMainGroupIndex(): Int =
        max(0, children.indexOfFirst { it is EnterpriseActionGroup<*> })
}
