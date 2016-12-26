package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene


class RestCallAction(
        val verb: HttpVerb,
        val path: RestPath,
        val parameters: List<out Param>
) : RestAction{


    override fun copy(): Action {
        return RestCallAction(verb, path, parameters.map(Param::copy))
    }

    override fun getName(): String {
        return "$verb:$path"
    }

    override fun execute() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun seeGenes(): List<out Gene> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}