package org.evomaster.core.problem.webfrontend

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.gui.GuiIndividual
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.ActionComponent
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class WebIndividual(
    children: MutableList<out ActionComponent>,
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(
        children,
        children.size,
        0
    )
) : GuiIndividual(
    children = children,
    childTypeVerifier = {
        EnterpriseActionGroup::class.java.isAssignableFrom(it)
                || DbAction::class.java.isAssignableFrom(it)
    },
    groups = groups
) {

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        TODO("Not yet implemented")
    }

    override fun size(): Int {
        TODO("Not yet implemented")
    }

    override fun verifyInitializationActions(): Boolean {
        TODO("Not yet implemented")
    }

    override fun seeMainExecutableActions(): List<WebAction> {
        return super.seeMainExecutableActions() as List<WebAction>
    }
}