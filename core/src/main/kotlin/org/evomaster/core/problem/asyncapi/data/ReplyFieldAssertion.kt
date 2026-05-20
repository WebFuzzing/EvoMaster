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
 * Coverage grew across milestones: M9-PR5 added REQUIRED + ENUM; M11-PR1
 * added MIN / MAX / MIN_LENGTH / MAX_LENGTH / FORMAT; M11-PR2 added
 * CONST / PATTERN / MULTIPLE_OF / ARRAY_MIN_ITEMS / ARRAY_MAX_ITEMS /
 * ARRAY_UNIQUE / DISCRIMINATOR plus support for nested dotted paths
 * (e.g. `comment.author.name`).
 */
data class ReplyFieldAssertion(
    /**
     * Property path on the reply object, dotted for nesting
     * (e.g. `comment.author.name`).
     */
    val path: String,
    val kind: Kind,
    /** Allowed values when [kind] is ENUM, CONST, or DISCRIMINATOR. */
    val expectedValues: List<String> = emptyList(),
    /** Numeric bound when [kind] is MIN / MAX / MULTIPLE_OF. */
    val numericBound: Double? = null,
    /** Integer bound when [kind] is MIN_LENGTH / MAX_LENGTH / ARRAY_MIN_ITEMS / ARRAY_MAX_ITEMS. */
    val lengthBound: Int? = null,
    /** Format name when [kind] is FORMAT (e.g. `date-time`, `email`, `uuid`). */
    val format: String? = null,
    /** Regex pattern when [kind] is PATTERN. */
    val pattern: String? = null
) {
    enum class Kind {
        /** Field must be present on the payload object. */
        REQUIRED,
        /** Field's textual value must be one of [expectedValues]. */
        ENUM,
        /** Field's textual value must equal the single value in [expectedValues]. */
        CONST,
        /** Field's textual value must match the regex [pattern]. */
        PATTERN,
        /** Field's numeric value must be >= [numericBound]. */
        MIN,
        /** Field's numeric value must be <= [numericBound]. */
        MAX,
        /** Field's numeric value must be a multiple of [numericBound]. */
        MULTIPLE_OF,
        /** Field's textual value length must be >= [lengthBound]. */
        MIN_LENGTH,
        /** Field's textual value length must be <= [lengthBound]. */
        MAX_LENGTH,
        /** Field's textual value must match the declared [format]. */
        FORMAT,
        /** Field's array length must be >= [lengthBound]. */
        ARRAY_MIN_ITEMS,
        /** Field's array length must be <= [lengthBound]. */
        ARRAY_MAX_ITEMS,
        /** Field's array must contain only distinct values. */
        ARRAY_UNIQUE,
        /**
         * Discriminator field's textual value must be one of [expectedValues]
         * (the declared `oneOf` variant names). Surfaces AsyncAPI/OpenAPI
         * discriminator semantics (M11-PR2 fix H).
         */
        DISCRIMINATOR
    }
}
