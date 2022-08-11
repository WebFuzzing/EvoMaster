package org.evomaster.core.search

class ActionDependentGroup(
    children: MutableList<out Action>,
    groups : GroupsOfChildren<out ActionComponent>
) : ActionTree(children, groups) {

    init {
        val main = groups.getGroup(GroupsOfChildren.MAIN)
        if(main.maxSize != 1){
           throw IllegalArgumentException("Main group for dependent actions must have exactly 1 element")
        }
    }

    override fun copyContent(): StructuralElement {
            return ActionDependentGroup(
                children.map { it.copy() } as MutableList<out Action>,
                groups!!.copy() as GroupsOfChildren<out ActionComponent>
            )
    }
}