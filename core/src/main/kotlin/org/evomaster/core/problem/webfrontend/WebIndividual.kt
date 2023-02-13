package org.evomaster.core.problem.webfrontend

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.gui.GuiIndividual
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.ActionComponent
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class WebIndividual(
    children: MutableList<out ActionComponent>,
    mainSize : Int = children.size,
    dbSize: Int = 0,
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(children,mainSize,dbSize)
) : GuiIndividual(
    children = children,
    childTypeVerifier = {
        EnterpriseActionGroup::class.java.isAssignableFrom(it)
                || DbAction::class.java.isAssignableFrom(it)
    },
    groups = groups
) {

    override fun copyContent(): Individual {
        return WebIndividual(
            children.map { it.copy() }.toMutableList() as MutableList<ActionComponent>,
            mainSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN),
            dbSize = groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SQL)
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