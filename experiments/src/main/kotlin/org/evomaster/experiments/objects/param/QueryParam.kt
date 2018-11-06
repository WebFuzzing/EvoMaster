package org.evomaster.experiments.objects.param

import org.evomaster.core.search.gene.Gene


class QueryParam(name: String, gene: Gene) : Param(name, gene){


    override fun copy(): Param {
        return QueryParam(name, gene.copy())
    }
}