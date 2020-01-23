package org.evomaster.core.problem.rest.param

import org.evomaster.core.search.gene.Gene

@Deprecated("Not needed any more with OpenApi V3, even when parsing old V2")
class FormParam (name: String, gene: Gene) : Param(name, gene){

    override fun copy(): Param {
        return FormParam(name, gene.copy())
    }
}