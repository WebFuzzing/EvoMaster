package org.evomaster.core.problem.asyncapi.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene

/**
 * Single parameter on an [org.evomaster.core.problem.asyncapi.data.AsyncAPIAction].
 * Always wraps exactly one top-level gene (the message payload, key, or correlation id).
 */
class AsyncAPIParam(name: String, gene: Gene) : Param(name, gene) {

    override fun copyContent(): AsyncAPIParam {
        return AsyncAPIParam(name, primaryGene().copy())
    }
}
