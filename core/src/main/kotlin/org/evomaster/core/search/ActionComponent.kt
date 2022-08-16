package org.evomaster.core.search


/**
 * Shared superclass to represent single [Action] and groups [ActionTree]
 */
abstract class ActionComponent(children: MutableList<out StructuralElement>) : StructuralElement(children){

    /**
     * Return a flattened reference to all actions in this component
     */
    abstract fun flatten() : List<Action>
}