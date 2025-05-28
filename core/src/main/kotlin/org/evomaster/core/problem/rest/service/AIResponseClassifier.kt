package org.evomaster.core.problem.rest.service

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

class AIResponseClassifier {

    fun updateModel(input: RestCallAction, output: RestCallResult){
        //TODO
    }

    fun classify(input: RestCallAction) : StatusGroup{

        //TODO
        return StatusGroup.G_2xx
    }

}