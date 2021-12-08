package org.evomaster.core.problem.graphql.param

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.search.gene.Gene


class GQInputParam(name: String, gene: Gene) : Param(name, gene) {

    override fun copyContent(): Param {
        return GQInputParam(name, gene.copyContent())
    }
}