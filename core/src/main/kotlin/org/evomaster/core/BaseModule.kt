package org.evomaster.core

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.evomaster.core.search.AdaptiveParameterControl
import org.evomaster.core.search.Randomness
import org.evomaster.core.search.SearchTimeController


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