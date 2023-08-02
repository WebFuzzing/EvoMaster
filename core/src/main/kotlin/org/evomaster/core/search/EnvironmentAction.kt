package org.evomaster.core.search

import org.evomaster.core.search.action.Action


/**
 * An action used to setup the environment of a test: eg databases and external services
 */
abstract class EnvironmentAction(children: List<StructuralElement>)  : Action(children){

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }
}