package org.evomaster.core.problem.rest.param

import org.evomaster.core.search.gene.Gene


abstract class Param(val gene : Gene) {

    abstract fun copy(): Param
}