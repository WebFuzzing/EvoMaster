package org.evomaster.core.problem.rest.service.classifier

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

interface AIModel {
    fun updateModel(input: RestCallAction, output: RestCallResult)
    fun classify(input: RestCallAction): StatusGroup
    fun probValidity(input: RestCallAction): Double
}