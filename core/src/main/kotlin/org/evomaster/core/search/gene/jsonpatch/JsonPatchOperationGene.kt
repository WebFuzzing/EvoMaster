package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * Base class for all JSON Patch operation genes.
 * Each operation gene knows its operation name (op) and carries the relevant field genes.
 */
abstract class JsonPatchOperationGene(
    name: String,
    val operationName: String,
    children: List<Gene>
) : CompositeFixedGene(name, children) {

    companion object {
        const val OP_ADD     = "add"
        const val OP_REMOVE  = "remove"
        const val OP_REPLACE = "replace"
        const val OP_MOVE    = "move"
        const val OP_COPY    = "copy"
        const val OP_TEST    = "test"
    }

    override fun checkForLocallyValidIgnoringChildren() = true

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        children.filter { it.isMutable() }.forEach { it.randomize(randomness, tryToForceNewValue) }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean = false
}