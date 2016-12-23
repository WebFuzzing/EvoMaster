package org.evomaster.core

import com.google.inject.AbstractModule
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController


class BaseModule : AbstractModule() {

    override fun configure() {

        bind(EMConfig::class.java)
                .asEagerSingleton()

        bind(SearchTimeController::class.java)
                .asEagerSingleton()

        bind(AdaptiveParameterControl::class.java)
                .asEagerSingleton()

        bind(Randomness::class.java)
                .asEagerSingleton()
    }
}