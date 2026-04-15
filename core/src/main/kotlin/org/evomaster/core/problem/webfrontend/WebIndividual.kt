package org.evomaster.core.problem.webfrontend

import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.gui.GuiIndividual
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement

class WebIndividual(
    sampleType: SampleType,
    children: MutableList<out ActionComponent>,
    mainSize : Int = children.size,
    sqlSize: Int = 0,
    mongoSize: Int = 0,
    redisSize: Int = 0,
    dnsSize: Int = 0,
    groups : GroupsOfChildren<StructuralElement> =
        getEnterpriseTopGroups(children,mainSize,sqlSize,mongoSize,redisSize,dnsSize, 0, 0)
) : GuiIndividual(
    sampleType = sampleType,
    children = children,
    childTypeVerifier = EnterpriseChildTypeVerifier(WebAction::class.java),
    groups = groups
) {

    override fun copyContent(): Individual {
        return WebIndividual(
            sampleType,
            children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
            mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN),
            sqlSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SQL),
            mongoSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_MONGO),
            redisSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_REDIS),
            dnsSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_DNS)
        )
    }

    override fun seeMainExecutableActions(): List<WebAction> {
        return super.seeMainExecutableActions() as List<WebAction>
    }
}
