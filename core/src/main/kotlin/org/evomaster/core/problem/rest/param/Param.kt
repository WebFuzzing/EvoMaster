package org.evomaster.core.problem.rest.param

import org.evomaster.core.search.gene.Gene


abstract class Param(val name: String, val gene : Gene) {

    init{
        if (name.isBlank()){
            throw IllegalArgumentException("Empty name")
        }
    }

    abstract fun copy(): Param
}