package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

/**
 * TODO documentation
 */
interface AIModel {

    /**
     * TODO documentation
     */
    fun updateModel(input: RestCallAction, output: RestCallResult)

    /**
     * TODO documentation
     */
    fun classify(input: RestCallAction): AIResponseClassification


}