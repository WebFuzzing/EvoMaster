package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.mutator.Mutator


abstract class SearchAlgorithm<T> where T : Individual {

    @Inject
    protected lateinit var sampler: Sampler<T>

    @Inject
    protected lateinit var ff: FitnessFunction<T>

    @Inject
    protected lateinit var randomness: Randomness

    @Inject
    protected lateinit var time: SearchTimeController

    @Inject
    protected lateinit var archive: Archive<T>

    @Inject
    protected lateinit var apc: AdaptiveParameterControl

    @Inject
    protected lateinit var config: EMConfig

    @Inject(optional = true)
    private lateinit var mutator: Mutator<T>

    private var lastSnapshot = 0

    protected fun getMutator(): Mutator<T> {
        return mutator
    }

    abstract fun searchOnce()

    abstract fun setupBeforeSearch()

    fun search(writeTests: (s: Solution<T>, t: String) -> Unit): Solution<T> {

        time.startSearch()

        setupBeforeSearch()

        while (time.shouldContinueSearch()) {

            searchOnce()

            if (needsToSnapshot()) {
                lastSnapshot = time.getElapsedSeconds()
                val partialSolution = archive.extractPartialSolution()
                writeTests(partialSolution, time.getElapsedSeconds().toString())
            }
        }

        return archive.extractSolution()
    }

    private fun needsToSnapshot(): Boolean {
        var isSnapshotEnabled = config.enableWriteSnapshotTests
        var snapshotPeriod = config.writeSnapshotTestsIntervalInSeconds

        return isSnapshotEnabled && time.getElapsedSeconds() - lastSnapshot > snapshotPeriod
    }

    abstract fun getType(): EMConfig.Algorithm
}
