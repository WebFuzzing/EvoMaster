package org.evomaster.core.problem.rest.classifier.deterministic.constraints

import org.evomaster.core.problem.rest.classifier.InputField
import org.evomaster.core.problem.rest.data.RestCallAction

interface ConstraintFor400 {

    /**
     * The input led to a 2xx response.
     * Remove all estimated constraints that would wrongly classify such a call as a 400
     */
    fun update2xx(input: RestCallAction)

    /**
     * If the input call would violate our model constraints, return which fields are involved in the violation.
     * If there is no violation, then the result is empty
     */
    fun checkUnsatisfiedConstraints(input: RestCallAction) : Set<InputField>
}