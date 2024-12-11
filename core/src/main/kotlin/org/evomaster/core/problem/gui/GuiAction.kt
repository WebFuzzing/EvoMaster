package org.evomaster.core.problem.gui

import org.evomaster.core.search.action.MainAction
import org.evomaster.core.search.StructuralElement


abstract class GuiAction(
    children: List<StructuralElement>
): MainAction(children) {


    /**
     * When a GUI action is created, what can be done depends on the current state of the GUI view.
     * This cannot be known at sampling time, but only during fitness evaluation, where all previous
     * actions have been executed
     */
    abstract fun isDefined() : Boolean


    /**
     * Determine whether the current action can be executed in the current context.
     * This might fail if modifications to previous actions lead to a different context (eg a different
     * page of the GUI where the action is not applicable)
     */
    abstract fun isApplicableInCurrentContext() : Boolean
}