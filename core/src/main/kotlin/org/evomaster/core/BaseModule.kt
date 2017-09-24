package org.evomaster.core

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.evomaster.core.search.service.*


class BaseModule(val args: Array<String>) : AbstractModule() {

    constructor() : this(emptyArray())

    override fun configure() {

        bind(SearchTimeController::class.java)
                .asEagerSingleton()

        bind(AdaptiveParameterControl::class.java)
                .asEagerSingleton()

        bind(Randomness::class.java)
                .asEagerSingleton()

        bind(IdMapper::class.java)
                .asEagerSingleton()

        bind(Statistics::class.java)
                .asEagerSingleton()

        bind(SearchStatusUpdater::class.java)
                .asEagerSingleton()
    }

    @Provides @Singleton
    fun getEMConfig() : EMConfig{
        val config = EMConfig()

        val parser = EMConfig.getOptionParser()
        val options = parser.parse(*args)

        config.updateProperties(options)
        return config
    }
}