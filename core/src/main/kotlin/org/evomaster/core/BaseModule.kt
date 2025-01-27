package org.evomaster.core

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.evomaster.core.output.service.NoTestCaseWriter
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.search.service.mutator.genemutation.ArchiveImpactSelector
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.monitor.SearchProcessMonitor
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.tracer.ArchiveMutationTrackService
import org.evomaster.core.search.tracer.TrackService
import org.evomaster.core.solver.SMTLibZ3DbConstraintSolver


/**
 * When we were the application, there is going to a be a set of
 * default beans/services which are used regardless of the kind
 * of testing we do.
 */
class BaseModule(val args: Array<String>, val noTests: Boolean = false) : AbstractModule() {

    constructor() : this(emptyArray())

    override fun configure() {

        bind(ExecutionPhaseController::class.java)
            .asEagerSingleton()

        bind(DataPool::class.java)
            .asEagerSingleton()

        bind(SearchGlobalState::class.java)
                .asEagerSingleton()

        bind(StringSpecializationArchive::class.java)
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

        bind(TrackService::class.java)
                .to(ArchiveMutationTrackService::class.java)
                .asEagerSingleton()

        bind(ArchiveMutationTrackService::class.java)
                .asEagerSingleton()

        bind(ArchiveImpactSelector::class.java)
                .asEagerSingleton()

        bind(ArchiveGeneMutator::class.java)
                .asEagerSingleton()

        bind(MutationWeightControl::class.java)
                .asEagerSingleton()

        bind(PartialOracles::class.java)
                .asEagerSingleton()

        bind(ExecutionInfoReporter::class.java)
                .asEagerSingleton()

        bind(SMTLibZ3DbConstraintSolver::class.java)
            .asEagerSingleton()

        //no longer needed if TestSuiteWriter is moved out?
//        if(noTests){
//            bind(TestCaseWriter::class.java)
//                    .to(NoTestCaseWriter::class.java)
//                    .asEagerSingleton()
//        }
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