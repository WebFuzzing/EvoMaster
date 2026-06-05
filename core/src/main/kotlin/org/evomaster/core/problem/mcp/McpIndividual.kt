package org.evomaster.core.problem.mcp

import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.ActionComponent

class McpIndividual(
    sampleType: SampleType,
    allActions: MutableList<out ActionComponent>,
    mainSize: Int = allActions.size,
    sqlSize: Int = 0,
    mongoSize: Int = 0,
    groups: GroupsOfChildren<StructuralElement> =
        getEnterpriseTopGroups(allActions, mainSize, sqlSize, mongoSize, 0, 0, 0, 0)
) : ApiWsIndividual(
    sampleType = sampleType,
    children = allActions,
    childTypeVerifier = EnterpriseChildTypeVerifier(McpAction::class.java),
    groups = groups
) {

    override fun copyContent(): Individual {
        return McpIndividual(
            sampleType,
            children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
            mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN)
        )
    }

    fun addMcpAction(relativePosition: Int = -1, action: McpAction) {
        addMainActionInEmptyEnterpriseGroup(relativePosition, action)
    }

    fun removeMcpActionAt(relativePosition: Int) {
        killChildByIndex(relativePosition)
    }

    override fun seeMainExecutableActions(): List<McpAction> {
        return super.seeMainExecutableActions() as List<McpAction>
    }
}
