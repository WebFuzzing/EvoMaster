package org.evomaster.core.problem.asyncapi.data

/**
 * One schema-derived assertion the test-case writer should emit against the
 * reply payload of a SUBSCRIBE_REPLY action.
 *
 * Pre-computed at build time so the writer can stay free of `AsyncAPISchema`
 * references; the builder walks the declared reply variant's `properties` and
 * pushes one of these per (field × declared facet) pair.
 *
 * The exhaustive ladder lives in the fitness layer
 * ([org.evomaster.core.problem.asyncapi.service.fitness.AbstractAsyncAPIFitness.replyFieldFacetTargets]).
 * The writer side only emits `REQUIRED` and `ENUM` checks for now because
 * those are by far the most readable in generated CI output; bounds / length /
 * format follow in a later iteration without further changes to this DTO.
 */
data class ReplyFieldAssertion(
    /** Property path on the reply object (top-level only for the starter). */
    val path: String,
    val kind: Kind,
    /** Allowed values when [kind] == ENUM. */
    val expectedValues: List<String> = emptyList()
) {
    enum class Kind { REQUIRED, ENUM }
}
