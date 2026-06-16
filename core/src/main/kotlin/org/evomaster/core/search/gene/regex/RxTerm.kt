package org.evomaster.core.search.gene.regex

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene


interface RxTerm {
    /**
     * Returns true if this gene can never produce a valid value,
     * for example an empty character class intersection like [a&&b].
     * Used at construction time to filter unsatisfiable branches from disjunctions.
     */
    fun isUnsatisfiable(): Boolean = false
}