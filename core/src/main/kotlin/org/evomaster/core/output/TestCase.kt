package org.evomaster.core.output

import org.evomaster.core.search.EvaluatedIndividual


class TestCase(val test: EvaluatedIndividual<*>, val name: String) {

    init {
        if(name.isBlank()){
            throw IllegalArgumentException("Blank name for test")
        }
    }


}