package org.evomaster.core

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.monitor.SearchProcessMonitor


/**
 * When we were the application, there is going to a be a set of
 * default beans/services which are used regardless of the kind
 * of testing we do.
 */
class BaseModule(val args: Array<String>) : AbstractModule() {

    constructor() : this(emptyArray())

    override fun configure() {

        bind(TestSuiteWriter::class.java)
                .asEagerSingleton()

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

        bind(SearchProcessMonitor::class.java)
                .asEagerSingleton()

        bind(ExtraHeuristicsLogger::class.java)
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