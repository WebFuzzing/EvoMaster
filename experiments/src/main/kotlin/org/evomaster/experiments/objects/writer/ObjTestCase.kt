package org.evomaster.experiments.objects.writer

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.EvaluatedIndividual


class ObjTestCase(val test: EvaluatedIndividual<*>, val name: String) {

    init {
        if(name.isBlank()){
            throw IllegalArgumentException("Blank name for test")
        }
    }

    /**
     * Check if any action requires a chain based on location headers:
     * eg a POST followed by a GET on the created resource
     */
    fun hasChainedLocations() : Boolean{
        return test.individual.seeActions().any { a ->
            a is RestCallAction && a.isLocationChained()
        }
    }
}