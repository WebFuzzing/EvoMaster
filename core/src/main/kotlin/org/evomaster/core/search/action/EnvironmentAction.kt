package org.evomaster.core.search.action

import org.evomaster.core.search.StructuralElement

/**
 * An action used to set up the environment of a test: eg databases and external services
 */
abstract class EnvironmentAction(children: List<StructuralElement>)  : Action(children){

    final override fun shouldCountForFitnessEvaluations(): Boolean {
        //Setup actions should never count for fitness evaluation
        return false
    }
}