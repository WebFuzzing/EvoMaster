package org.evomaster.core.problem.asyncapi.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene

/**
 * One part of a message the search can vary: its payload, or its headers.
 *
 * The two are separate parameters rather than one because they travel separately on the wire:
 * a transport with metadata puts headers beside the body rather than inside it.
 */
class AsyncApiParam(
    name: String,
    gene: Gene,
    /**
     * Whether the document says nothing about the shape of this payload, so that it may be
     * replaced (see [UpdateForAsyncApiPayloadParam]). Only ever true for a [PAYLOAD], and only
     * when expanding payloads was asked for. A replacement payload is not opaque.
     */
    val opaque: Boolean = false
) : Param(name, gene) {

    companion object {
        const val PAYLOAD = "payload"

        const val HEADERS = "headers"
    }

    init {
        if (opaque && name != PAYLOAD) {
            throw IllegalArgumentException("Only a payload can be opaque, not '$name'")
        }
    }

    override fun copyContent(): AsyncApiParam = AsyncApiParam(name, gene.copy(), opaque)
}
