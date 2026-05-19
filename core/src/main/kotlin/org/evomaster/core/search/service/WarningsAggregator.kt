package org.evomaster.core.search.service

import org.evomaster.core.search.warning.GeneralWarning
import java.util.concurrent.CopyOnWriteArraySet

class WarningsAggregator {

    private val warnings : MutableSet<GeneralWarning> = CopyOnWriteArraySet()

    fun addWarning(warning: GeneralWarning) {
        warnings.add(warning)
    }

    fun getWarnings() : Set<GeneralWarning> = warnings
}