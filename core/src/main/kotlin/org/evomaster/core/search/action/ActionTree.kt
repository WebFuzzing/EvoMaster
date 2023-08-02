package org.evomaster.core.search.action

import org.evomaster.core.search.GroupsOfChildren

/**
 * Tree/group of actions that are strongly related.
 * This group can have internal groups, ie internal [ActionTree].
 *
 * When executing a test case, such tree-structure groups need to be flattened
 */
abstract class ActionTree(
    children: MutableList<out ActionComponent>,
    childTypeVerifier: (Class<*>) -> Boolean = {k -> ActionComponent::class.java.isAssignableFrom(k)},
    groups : GroupsOfChildren<out ActionComponent>? = null
) : ActionComponent(
    children,
    childTypeVerifier,
    groups
){

    override fun flatten(): List<Action> {
        return children.flatMap { (it as ActionComponent).flatten()}
    }

    override fun flatView(): List<ActionComponent> {
        return listOf(this).plus(children.flatMap { (it as ActionComponent).flatView() })
    }
}