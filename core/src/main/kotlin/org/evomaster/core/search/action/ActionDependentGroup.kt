package org.evomaster.core.search.action

import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.StructuralElement

open class ActionDependentGroup(
    children: MutableList<out Action>,
    childTypeVerifier: (Class<*>) -> Boolean = {k -> Action::class.java.isAssignableFrom(k)},
    groups: GroupsOfChildren<out ActionComponent>
) : ActionTree(children, childTypeVerifier, groups) {

    init {
        val main = groups.getGroup(GroupsOfChildren.MAIN)
        if (main.maxSize != 1) {
            throw IllegalArgumentException("Main group for dependent actions must have exactly 1 element")
        }
    }

    override fun copyContent(): StructuralElement {

        val k = children.map { it.copy() } as MutableList<out Action>

        return ActionDependentGroup(
            k,
            childTypeVerifier,
            groupsView()!!.copy(k) as GroupsOfChildren<out ActionComponent>
        )
    }
}