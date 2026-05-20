package org.evomaster.core.problem.asyncapi.data

/**
 * One schema-derived assertion the test-case writer should emit against the
 * reply (or SUBSCRIBE_OUTPUT) payload.
 *
 * Pre-computed at build time so the writer can stay free of `AsyncAPISchema`
 * references; the builder walks the declared message's `properties` and
 * pushes one of these per (field × declared facet) pair.
 *
 * The exhaustive ladder lives in the fitness layer
 * ([org.evomaster.core.problem.asyncapi.service.fitness.AbstractAsyncAPIFitness.replyFieldFacetTargets]).
 * Initially the writer emitted only `REQUIRED` + `ENUM`; the later extension
 * adds `MIN`, `MAX`, `MIN_LENGTH`, `MAX_LENGTH`, and `FORMAT` checks so the
 * generated tests surface the same facets the fitness targets exercise.
 */
data class ReplyFieldAssertion(
    /** Property path on the reply object (top-level only for the starter). */
    val path: String,
    val kind: Kind,
    /** Allowed values when [kind] == ENUM. */
    val expectedValues: List<String> = emptyList(),
    /** Numeric bound when [kind] is MIN or MAX (inclusive). */
    val numericBound: Double? = null,
    /** Integer bound when [kind] is MIN_LENGTH or MAX_LENGTH (inclusive). */
    val lengthBound: Int? = null,
    /** Format name when [kind] is FORMAT (e.g. `date-time`, `email`, `uuid`). */
    val format: String? = null
) {
    enum class Kind {
        /** Field must be present on the payload object. */
        REQUIRED,
        /** Field's textual value must be one of [expectedValues]. */
        ENUM,
        /** Field's numeric value must be >= [numericBound]. */
        MIN,
        /** Field's numeric value must be <= [numericBound]. */
        MAX,
        /** Field's textual value length must be >= [lengthBound]. */
        MIN_LENGTH,
        /** Field's textual value length must be <= [lengthBound]. */
        MAX_LENGTH,
        /** Field's textual value must match the declared [format]. */
        FORMAT
    }
}
