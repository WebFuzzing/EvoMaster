package org.evomaster.core.problem.rest.service

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.Test
import kotlin.random.Random


class AIResponseClassifier {

    fun updateModel(input: RestCallAction, output: RestCallResult){
        //TODO
    }

    fun classify(input: RestCallAction) : StatusGroup{

        //TODO
        return StatusGroup.G_2xx

    }

    // return the probability of acceptance based on the classifier
    fun probValidity(input: RestCallAction) : Double{

        // TODO
        // this part must be fixed based on the gaussian model
        // for now we just generate a random number assuming the model
        // provided that
        val prob2xx = Random.nextDouble(0.0, 1.0)
        return prob2xx

    }

}