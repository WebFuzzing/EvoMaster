package org.evomaster.core.problem.webfrontend

import org.evomaster.core.sql.SqlAction
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.gui.GuiIndividual
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class WebIndividual(
    sampleType: SampleType,
    children: MutableList<out ActionComponent>,
    mainSize : Int = children.size,
    sqlSize: Int = 0,
    mongoSize: Int = 0,
    dnsSize: Int = 0,
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(children,mainSize,sqlSize,mongoSize,dnsSize)
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
            dnsSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_DNS)
        )
    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        //TODO
        return listOf()
    }


    override fun seeMainExecutableActions(): List<WebAction> {
        return super.seeMainExecutableActions() as List<WebAction>
    }
}
