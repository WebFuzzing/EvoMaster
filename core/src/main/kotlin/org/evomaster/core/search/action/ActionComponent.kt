package org.evomaster.core.search.action

import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.StructuralElement

/**
 * Shared superclass to represent single [Action] and groups [ActionTree]
 */
abstract class ActionComponent(
    children: MutableList<out StructuralElement>,
    childTypeVerifier: (Class<*>) -> Boolean,
    groups : GroupsOfChildren<out StructuralElement>? = null
) : StructuralElement(children, childTypeVerifier, groups as GroupsOfChildren<StructuralElement>?){


    /**
     * Return a flattened reference to all actions in this component
     */
    abstract fun flatten() : List<Action>

    /**
     * @return a recursive list of all nested action component
     */
    abstract fun flatView() : List<ActionComponent>
}