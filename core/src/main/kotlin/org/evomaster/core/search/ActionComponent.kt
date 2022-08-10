package org.evomaster.core.search


/**
 * Shared superclass to represent single [Action] and groups [ActionTree]
 */
abstract class ActionComponent(
        children: MutableList<out StructuralElement>,
        groups : GroupsOfChildren<StructuralElement>? = null) : StructuralElement(children, groups){

    /**
     * Return a flattened reference to all actions in this component
     */
    abstract fun flatten() : List<Action>
}