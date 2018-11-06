package org.evomaster.experiments.objects.param

import org.evomaster.core.search.gene.Gene


class BodyParam(gene: Gene) : Param("body", gene){

    override fun copy(): Param {
        return BodyParam(gene.copy())
    }
}