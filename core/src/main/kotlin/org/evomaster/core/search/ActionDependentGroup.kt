package org.evomaster.core.search

open class ActionDependentGroup(
    children: MutableList<out Action>,
    groups: GroupsOfChildren<out ActionComponent>
) : ActionTree(children, groups) {

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
            groupsView()!!.copy(k) as GroupsOfChildren<out ActionComponent>
        )
    }
}