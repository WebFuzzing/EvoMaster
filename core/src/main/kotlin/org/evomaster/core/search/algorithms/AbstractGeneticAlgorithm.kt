package org.evomaster.core.search.algorithms

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Solution
import com.google.inject.Inject
import org.evomaster.core.search.algorithms.strategy.suite.CrossoverOperator
import org.evomaster.core.search.algorithms.strategy.suite.MutationEvaluationOperator
import org.evomaster.core.search.algorithms.strategy.suite.DefaultMutationEvaluationOperator
import org.evomaster.core.search.algorithms.strategy.suite.SelectionStrategy
import org.evomaster.core.search.algorithms.strategy.suite.TournamentSelectionStrategy
import org.evomaster.core.search.algorithms.strategy.suite.DefaultCrossoverOperator
import org.evomaster.core.search.algorithms.observer.GAObserver

/**
 * Abstract base class for implementing Genetic Algorithms (GAs) in EvoMaster.
 *
 * Provides core logic for:
 * - Population initialization,
 * - Elitism,
 * - Mutation and crossover operators,
 * - Tournament selection.
 *
 * Concrete subclasses (e.g., StandardGeneticAlgorithm) should implement
 * the specifics of how the population is evolved in each generation
 * (typically by overriding searchOnce).
 *
 * @param T The type of individual (e.g., representing a test suite or test case).
 */
abstract class AbstractGeneticAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    /** The current population of evaluated individuals (test suites). */
    protected val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    /**
     * Frozen set of targets (coverage objectives) to score against during the current generation.
     * Captured at the start of a generation and kept constant until the generation ends.
     */
    protected var frozenTargets: Set<Int> = emptySet()

    protected var selectionStrategy: SelectionStrategy = TournamentSelectionStrategy()

    protected var crossoverOperator: CrossoverOperator = DefaultCrossoverOperator()

    protected var mutationOperator: MutationEvaluationOperator = DefaultMutationEvaluationOperator()

    /** Optional observers for GA events (test/telemetry). */
    protected val observers: MutableList<GAObserver<T>> = mutableListOf()

    fun addObserver(observer: GAObserver<T>) {
        observers.add(observer)
    }

    fun removeObserver(observer: GAObserver<T>) {
        observers.remove(observer)
    }

    /** Call at the start of each searchOnce to begin a new generation scope. */
    protected fun beginGeneration() {
        observers.forEach { it.onGenerationStart() }
    }

    /** Call at the end of each searchOnce to report generation aggregates. */
    protected fun endGeneration() {
        val snapshot = population.toList()
        val bestScore = snapshot.maxOfOrNull { score(it) } ?: 0.0
        observers.forEach { it.onGenerationEnd(snapshot, bestScore) }
    }

    /** Start a new step inside current iteration. */
    protected fun beginStep() {
        observers.forEach { it.onStepStart() }
    }

    /** End current step and report aggregates. */
    protected fun endStep() {
        observers.forEach { it.onStepEnd() }
    }

    /**
     * Called once before the search begins. Clears any old population and initializes a new one.
     */
    override fun setupBeforeSearch() {
        population.clear()
        initPopulation()
    }

    /**
     * Initializes the population with randomly sampled suites up to the configured population size.
     * Stops early if the time budget is exhausted.
     */
    protected open fun initPopulation() {
        val n = config.populationSize

        for (i in 1..n) {
            population.add(sampleSuite())

            if (!time.shouldContinueSearch()) {
                break
            }
        }
    }

    /**
     * Forms the starting point of the next generation's population.
     * Applies elitism: retains a fixed number of top individuals based on fitness.
     *
     * @param population The current generation.
     * @return A mutable list containing elite individuals for the next generation.
     */
    protected fun formTheNextPopulation(population: MutableList<WtsEvalIndividual<T>>): MutableList<WtsEvalIndividual<T>> {
        val nextPop: MutableList<WtsEvalIndividual<T>> = mutableListOf()

        if (config.elitesCount > 0 && population.isNotEmpty()) {
            val sortedPopulation = population.sortedByDescending { score(it) }
            val elites = sortedPopulation.take(config.elitesCount)
            nextPop.addAll(elites)
        }

        return nextPop
    }

    /**
     * Applies mutation to a given individual.
     *
     * The mutation operator is chosen randomly from:
     * - "del": Delete a random element from the suite (if more than one).
     * - "add": Add a newly sampled element (if below max suite size).
     * - "mod": Modify an existing element using the mutator.
     *
     * This method modifies the individual in-place.
     */
    protected fun mutate(wts: WtsEvalIndividual<T>) {
        mutationOperator.mutateEvaluateAndArchive(
            wts,
            config,
            randomness,
            getMutatator(),
            ff,
            sampler,
            archive
        )
        // notify observers
        observers.forEach { it.onMutation(wts) }
    }

    /**
     * Applies crossover between two individuals.
     *
     * Swaps elements in their suites up to a randomly chosen split point
     * (bounded by the size of the smaller suite).
     */
    protected fun xover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>) {
        crossoverOperator.applyCrossover(x, y, randomness)
        // notify observers
        observers.forEach { it.onCrossover(x, y) }
    }

    /**
     * Allows tests or callers to override GA operators without DI.
     */
    fun useSelectionStrategy(strategy: SelectionStrategy) { this.selectionStrategy = strategy }
    fun useCrossoverOperator(operator: CrossoverOperator) { this.crossoverOperator = operator }
    fun useMutationOperator(operator: MutationEvaluationOperator) { this.mutationOperator = operator }

    /**
     * Selects one individual using tournament selection.
     *
     * A subset of the population is randomly selected, and the individual with the
     * highest fitness among them is chosen. Falls back to random selection if needed.
     */
    protected fun tournamentSelection(): WtsEvalIndividual<T> {
        val sel = selectionStrategy.select(population, config.tournamentSize, randomness, ::score)
        observers.forEach { it.onSelection(sel) }
        return sel
    }

    /**
     * Samples a new individual suite consisting of 1 to maxSearchSuiteSize elements.
     *
     * Each element is generated by sampling and evaluated via the fitness function.
     * Stops early if the time budget is exceeded.
     */
    protected fun sampleSuite(): WtsEvalIndividual<T> {
        val n = 1 + randomness.nextInt(config.maxSearchSuiteSize)
        val suite = WtsEvalIndividual<T>(mutableListOf())

        for (i in 1..n) {
            ff.calculateCoverage(sampler.sample(), modifiedSpec = null)?.run {
                if (config.gaSolutionSource != EMConfig.GASolutionSource.POPULATION) {
                    archive.addIfNeeded(this)
                }
                suite.suite.add(this)
            }

            if (!time.shouldContinueSearch()) {
                break
            }
        }

        return suite
    }

    /**
     * Combined fitness of a suite computed only over [frozenTargets] when set; otherwise full combined fitness.
     */
    public fun score(w: WtsEvalIndividual<T>): Double {
        if (w.suite.isEmpty()) {
            return 0.0
        }

        if (frozenTargets.isEmpty()) {
            return w.calculateCombinedFitness()
        }

        val fv = w.suite.first().fitness.copy()
        w.suite.forEach { ei -> fv.merge(ei.fitness) }
        val view = fv.getViewOfData()
        var sum = 0.0
        frozenTargets.forEach { t ->
            val comp = view[t]
            if (comp != null){
                sum += comp.score
            }
        }
        return sum
    }

    /**
     * For GA algorithms, optionally build the final solution from the final population
     * instead of the archive, controlled by config.gaSolutionSource.
     */
    override fun buildSolution(): Solution<T> {
        return if (config.gaSolutionSource == EMConfig.GASolutionSource.POPULATION) {
            val best = population.maxByOrNull { it.calculateCombinedFitness() }
            val individuals = (best?.suite ?: mutableListOf())
            Solution(
                individuals.toMutableList(),
                config.outputFilePrefix,
                config.outputFileSuffix,
                org.evomaster.core.output.Termination.NONE,
                listOf(),
                listOf()
            )
        } else {
            super.buildSolution()
        }
    }

    /**
     * Exposes a read-only view of the current population for observability/tests.
     * Returns an immutable copy to prevent external mutations of internal state.
     */
    fun getViewOfPopulation(): List<WtsEvalIndividual<T>> = population.toList()
}
