package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.FeedbackDirectedSampling.FOCUSED_QUICKEST
import org.evomaster.core.EMConfig.FeedbackDirectedSampling.LAST
import org.evomaster.core.Lazy
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.*
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
import org.evomaster.core.search.service.IdMapper.Companion.LOCAL_OBJECTIVE_KEY
import org.evomaster.core.search.service.monitor.SearchProcessMonitor
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.tracer.ArchiveMutationTrackService
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


class Archive<T> where T : Individual {

    companion object{
        private val log = LoggerFactory.getLogger(Archive::class.java)
    }

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var dpc: AdaptiveParameterControl

    @Inject
    private lateinit var idMapper: IdMapper

    @Inject
    private lateinit var time: SearchTimeController

    @Inject
    private lateinit var processMonitor: SearchProcessMonitor

    @Inject
    private lateinit var tracker : ArchiveMutationTrackService

    /**
     * Key -> id of the target
     *
     * Value -> sorted list of best individuals for that target
     */
    private val populations = mutableMapOf<Int, MutableList<EvaluatedIndividual<T>>>()

    /**
     * Key -> id of the target
     *
     * Value -> how often we sampled from the buffer for that target since
     *          last fitness improvement.
     *          Note: such counter will be reset when a fitness improvement
     *          is obtained for that target is obtained.
     *          This means that an infeasible / hard target will not get
     *          its counter reset once the final local optima is reached
     */
    private val samplingCounter = mutableMapOf<Int, Int>()


    /**
     * Key -> id of the target
     *
     * Value -> keep track of how long (in number of sampled individuals)
     *          it took last time there was an improvement for this target.
     */
    private val lastImprovement = mutableMapOf<Int, Int>()


    /**
     * Id of last target used for sampling
     */
    private var lastChosen: Int? = null

    data class CoveredStatisticsBySeededTests<G> (val coveredTargets: List<Int>, val uniquePopulationsDuringSeeding : List<EvaluatedIndividual<G>>) where G: Individual

    private var coveredStatisticsBySeededTests : CoveredStatisticsBySeededTests<T>? = null


    /**
     * Kill all populations.
     * This is meanly needed for minimization phase, in which archive needs to be cleared and
     * tests re-added to it
     */
    fun clearPopulations(){
        populations.clear()
    }


    fun extractSolution(): Solution<T> {
        val uniques = getUniquePopulation()

        return Solution(
            uniques.toMutableList(),
            config.outputFilePrefix,
            config.outputFileSuffix,
            Termination.NONE,
            coveredStatisticsBySeededTests?.uniquePopulationsDuringSeeding?: listOf<EvaluatedIndividual<T>>(),
            coveredStatisticsBySeededTests?.coveredTargets?: listOf()
        )
    }


    fun archiveCoveredStatisticsBySeededTests(){
        if (coveredStatisticsBySeededTests != null){
            throw IllegalStateException("`archiveCoveredStatisticsBySeededTests` can only be performed once")
        }

        val current = extractSolution()
        coveredStatisticsBySeededTests = CoveredStatisticsBySeededTests(
            coveredTargets = current.overall.getViewOfData().filter { it.value.score == FitnessValue.MAX_VALUE }.keys.toList(),
            if (config.exportTestCasesDuringSeeding) current.individuals.map { it.copy() } else emptyList()
        )

    }

    fun anyTargetsCoveredSeededTests () = coveredStatisticsBySeededTests != null && coveredStatisticsBySeededTests!!.coveredTargets.isNotEmpty()


    fun getCopyOfUniqueCoveringIndividuals() : List<T>{
        return getUniquePopulation().map { it.individual.copy() as T }
    }

    private fun getUniquePopulation(): MutableSet<EvaluatedIndividual<T>> {

        /*
            Note: no equals() is defined, so Set is based
            on refs to the heap.
            This is not an issue, as each individual is copied
            when sampled.
            Here, as an individual can go to many populations,
            we want to avoid counting it several times.
         */
        val uniques = mutableSetOf<EvaluatedIndividual<T>>()

        populations.entries.forEach { e ->
            if (isCovered(e.key)) {
                val ind = e.value[0]
                uniques.add(ind)
            }
        }

        return uniques
    }


    fun isEmpty() = populations.isEmpty()

    /**
     * Get a copy of an individual in the archive.
     * Different kinds of heuristics are used to choose
     * the best "candidate" most useful for the search
     */
    fun sampleIndividual(): EvaluatedIndividual<T> {

        if (isEmpty()) {
            throw IllegalStateException("Empty archive")
        }

        var toChooseFrom = notCoveredTargets()
        if (toChooseFrom.isEmpty()) {
            //this means all current targets are covered
            toChooseFrom = populations.keys.toSet()
        }


        val chosenTarget = chooseTarget(toChooseFrom)
        lastChosen = chosenTarget

        val candidates = populations[chosenTarget] ?:
                //should never happen, unless of bug
                throw IllegalStateException("Target $chosenTarget has no candidate individual")


        incrementCounter(chosenTarget)

        sortAndShrinkIfNeeded(candidates, chosenTarget)

        val notTimedOut = candidates.filter {
            !it.seeResults().any { res -> res is HttpWsCallResult && res.getTimedout() }
        }

        /*
            If possible avoid sampling tests that did timeout
         */
        val chosen = if (!notTimedOut.isEmpty()) {
            randomness.choose(notTimedOut)
        } else {
            randomness.choose(candidates)
        }

        val copy = chosen.copy(tracker.getCopyFilterForEvalInd(chosen))
        copy.individual.populationOrigin = idMapper.getDescriptiveId(chosenTarget)

        return copy
    }

    private fun chooseTarget(toChooseFrom: Set<Int>): Int {

        if(!config.isMIO()){
            return  randomness.choose(toChooseFrom)
        }

        return when (config.feedbackDirectedSampling) {
            LAST -> toChooseFrom.minByOrNull {
                val counter = samplingCounter.getOrDefault(it, 0)
                val p = populations[it]!!
                val time = p[p.lastIndex].executionTimeMs //time of best individual
                if(!config.useTimeInFeedbackSampling || time == Long.MAX_VALUE){
                     counter.toLong()
                } else {
                    /*
                    WARNING: this does introduce some form of non-determinism,
                    as timestamps can vary.
                 */
                    (counter * time)
                }
            }!!
            FOCUSED_QUICKEST ->
                handleFocusedQuickest(toChooseFrom)
            else ->
                randomness.choose(toChooseFrom)
        }
    }

    private fun handleFocusedQuickest(toChooseFrom: Set<Int>): Int {

        val lc = lastChosen

        if (lc != null
                && toChooseFrom.contains(lc)
                /*
                    the X can happen if there was never an improvement.
                    so we still want to try 2X times before going to another
                    one
                 */
                && (samplingCounter[lc] ?: 0) < (lastImprovement[lc] ?: 10) * 2
                ) {
            return lc
        }

        /*
        We can't reuse the previous target. Need to pick up
        a new one
        */

        val index = toChooseFrom
                .filter {
                    val previous = lastImprovement[it]
                    previous != null &&
                            samplingCounter[it]!! < previous * 2
                }
                .minByOrNull { lastImprovement[it]!! }

        return index ?: toChooseFrom.minByOrNull {
            samplingCounter.getOrDefault(it, 0)
        }!!
    }

    /**
     * update counter by 1
     */
    private fun incrementCounter(target: Int) {
        samplingCounter.putIfAbsent(target, 0)
        val counter = samplingCounter[target]!!

        val delta = getWeightToAdd(target)
        samplingCounter[target] = counter + delta
    }

    private fun getWeightToAdd(target: Int) : Int {
        if(! config.useWeightedSampling){
            return 1
        }

        val id = idMapper.getDescriptiveId(target)
        if(id.startsWith(ObjectiveNaming.BRANCH)
                || id.startsWith(ObjectiveNaming.METHOD_REPLACEMENT)
                || id.startsWith(ObjectiveNaming.NUMERIC_COMPARISON)){
            return 1
        }

        return 10
    }

    private fun reportImprovement(target: Int) {

        val counter = samplingCounter.getOrDefault(target, 0)
        lastImprovement.put(target, counter)
        samplingCounter.put(target, 0)
    }

    /**
     * Useful for debugging
     */
    fun encounteredTargetDescriptions(): List<String> {

        return populations.entries
                .map { e -> "key ${e.key}: ${idMapper.getDescriptiveId(e.key)} , size=${e.value.size}" }
                .sorted()
    }

    /**
     * Useful for debugging
     */
    fun reachedTargetHeuristics(): List<String> {

        return populations.entries
                .map { e -> "key ${e.key} -> best heuristics=${e.value.map { it.fitness.computeFitnessScore() }.maxOrNull()}" }
                .sorted()
    }

    fun numberOfCoveredTargets(): Int {
        return populations.keys.stream().filter { isCovered(it) }.count().toInt()
    }

    fun numberOfReachedButNotCoveredTargets(): Int {
        return populations.keys.stream().filter { ! isCovered(it) }.count().toInt()
    }

    fun numberOfReachedTargets() : Int = populations.size

    fun averageTestSizeForReachedButNotCovered() : Double {
        return populations.entries
                .filter { ! isCovered(it.key) }
                .flatMap { it.value }
                .map { it.individual.size() }
                .average()
    }

    /**
     * Get all known targets that are not fully covered
     *
     * @return a list of ids
     */
    fun notCoveredTargets(): Set<Int> {

        /*
            FIXME: performance, use cache for non-covered.
            As we can have 10s of thousands of covered targets,
            iterating over them is expensive
         */

        return populations.keys.filter { !isCovered(it) }.toSet()
    }

    /**
     * Get all known targets that are fully covered
     *
     * @return a list of ids
     */
    fun coveredTargets(): Set<Int> {
        return populations.keys.filter { isCovered(it) }.toSet()
    }


    fun wouldReachNewTarget(ei: EvaluatedIndividual<T>): Boolean {

        return ei.fitness.getViewOfData()
                .filter { it.value.score > 0.0 }
                .map { it.key }
                .any { populations[it]?.isEmpty() ?: true }
    }

    fun identifyNewTargets(ei: EvaluatedIndividual<T>, targetInfo: MutableMap<Int, EvaluatedMutation>) {

        ei.fitness.getViewOfData()
                .filter { it.value.score > 0.0 && populations[it.key]?.isEmpty() ?: true}
                .forEach { t->
                    targetInfo[t.key] = EvaluatedMutation.NEWLY_IDENTIFIED
                }
    }

    /**
     * @return true if the new individual was added to the archive
     */
    fun addIfNeeded(ei: EvaluatedIndividual<T>): Boolean {

        val copy = ei.copy(tracker.getCopyFilterForEvalInd(ei))

        var added = false
        var anyBetter = false

        for ((k, v) in ei.fitness.getViewOfData()) {

            if (v.score == 0.0) {
                /*
                    No point adding an individual with no impact
                    on a given target
                 */
                continue
            }

            val current = populations.getOrPut(k, { mutableListOf() })

            //ind does reach a new target?
            if (current.isEmpty()) {
                current.add(copy)
                added = true
                time.newActionImprovement()
                reportImprovement(k)

                if (isCovered(k)) {
                    time.newCoveredTarget()
                }

                continue
            }

            val maxed = FitnessValue.isMaxValue(v.score)

            if (isCovered(k) && maxed) {
                /*
                    Target is already covered. But could it
                    be that new individual covers it as well,
                    and it is better?

                    Recall: during the search, the fitness score could be
                    partial, so this check on collateral coverage likely
                    will not be so effective
                 */
                Lazy.assert{current.size == 1} //if covered, should keep only one solution in buffer

                val shorter = copy.individual.size() < current[0].individual.size()
                val sameLengthButBetterScore = (copy.individual.size() == current[0].individual.size())
                        && (copy.fitness.computeFitnessScore() > current[0].fitness.computeFitnessScore())

                /*
                 * Once a target is covered, we check if can cover it with a new test that is shorter.
                 * Given two tests covering the same target, both with same length, then we prefer
                 * the one that has most collateral coverage
                 */
                if (shorter || sameLengthButBetterScore) {
                    current[0] = copy
                    added = true
                    time.newActionImprovement()
                    reportImprovement(k)
                }
                continue
            }

            if (maxed) {
                current.clear() //remove all existing non-optimal solutions
                current.add(copy)
                added = true
                time.newActionImprovement()
                reportImprovement(k)
                time.newCoveredTarget()
                continue
            }


            //handle regular case.
            sortAndShrinkIfNeeded(current, k)

            /*
                as the population are internally sorted by fitness, the individual
                at position [0] would be the worst
             */

            val curr = current[0]
            Lazy.assert {
                curr.fitness.size == curr.individual.size().toDouble()
                        &&
                copy.fitness.size == copy.individual.size().toDouble()
            }

            /*
              config.minimumSizeControl = 2 is to
                avoid reducing tests to size 1 if extra was better.
                With at least 2 actions, we can have a WRITE followed by a READ
            */
            val better = copy.fitness.betterThan(k, curr.fitness, config.secondaryObjectiveStrategy, config.bloatControlForSecondaryObjective, config.minimumSizeControl)

            anyBetter = anyBetter || better

            if (better) {
                time.newActionImprovement()
                reportImprovement(k)
            }

            val limit = dpc.getArchiveTargetLimit()

            /*
             individual can be added only if the target k is not covered.
             If a target is covered and a 'better'(e.g., shorter) individual appears,
             it would be handled as replacement.
             */
            if (!isCovered(k) && current.size < limit) {
                //we have space in the buffer, regardless of fitness
                current.add(copy)
                added = true

                continue
            }

            val equivalent = copy.fitness.equivalent(k, curr.fitness, config.secondaryObjectiveStrategy)

            if(config.discoveredInfoRewardedInFitness){

                val worst = current[0]
                val x = copy.individual.numberOfDiscoveredInfoFromTestExecution()
                val y = worst.individual.numberOfDiscoveredInfoFromTestExecution()

                if(!better && equivalent &&  x < y){
                    /*
                        Do not replace if it is "equivalent" but has fewer discoveries
                     */
                    continue
                }
            }


            if (better || equivalent) {
                /*
                    replace worst element, if copy is not worse than it (but not necessarily better).
                    However this is base on heuristics values and size, but NOT execution time

                    TODO would it makes sense to do something like subsumes() where
                    execution time is taken into account?
                 */
                current[0] = copy
                added = true
            }
        }
        processMonitor.record(added, anyBetter, ei)

        /*
            TODO should log them to a file
        */
        //LoggingUtil.getInfoLogger().info("$added $anyBetter ${ei.individual.populationOrigin}")

        ei.hasImprovement = anyBetter
        return added
    }

    /*
       Ascending sort based on heuristics and, if same value, on negation of size.
       Worst element will be the first, best the last.
       Resize the list if needed
     */
    private fun sortAndShrinkIfNeeded(list: MutableList<EvaluatedIndividual<T>>, target: Int) {

        /*
            First look at heuristics for the target.
            That is most important value.
            In case of same, then look at the extra heuristics.
            If all the same, then do prefer shorter tests.
         */

        list.sortWith(compareBy<EvaluatedIndividual<T>>
        { it.fitness.getHeuristic(target) }
                .thenComparator { a, b -> a.fitness.compareExtraToMinimize(target, b.fitness, config.secondaryObjectiveStrategy) }
                .thenBy { -it.individual.size() }
                .thenBy{ if(config.useTimeInFeedbackSampling) -it.executionTimeMs else 0L})

        val limit = dpc.getArchiveTargetLimit()
        while (list.size > limit) {
            //remove worst, ie the one with lowest heuristic value
            list.removeAt(0)
        }
    }

    fun isCovered(target: Int): Boolean {

        val current = populations[target] ?: return false

        if (current.size != 1) {
            return false
        }

        return current[0].fitness.doesCover(target)
    }

    /**
     * useful for debugging
     */
    fun getReachedTargetHeuristics(target: Int) : Double?{
        return populations[target]?.map { v-> v.fitness.getHeuristic(target) }?.maxOrNull()
    }

    /**
     * @return current population
     */
    fun getSnapshotOfBestIndividuals(): Map<Int, MutableList<EvaluatedIndividual<T>>>{
        return populations
    }

    /**
     * @return current samplingCounter
     */
    fun getSnapshotOfSamplingCounter() : Map<Int, Int>{
        return samplingCounter
    }

    /**
     * @return a list of pairs which is composed of target id (first) and corresponding tests (second)
     */
    fun exportCoveredTargetsAsPair(solution: Solution<*>, includeTargetsCoveredBySeededTests: Boolean? = null) : List<Pair<String, List<Int>>>{

        return populations.keys
                .asSequence()
                .filter {
                    isCovered(it) && (includeTargetsCoveredBySeededTests == null
                            || (coveredStatisticsBySeededTests==null)
                            || (if (includeTargetsCoveredBySeededTests) coveredStatisticsBySeededTests!!.coveredTargets.contains(it) else !coveredStatisticsBySeededTests!!.coveredTargets.contains(it)))
                }
                .map { t->
                    Pair(idMapper.getDescriptiveId(t), solution.individuals.mapIndexed { index, f-> if (f.fitness.doesCover(t)) index else -1 }.filter { it != -1 })
                }.toList()
    }

    /**
     * @return an existing ImpactsOfIndividual which includes same action with [other]
     */
    fun findImpactInfo(other: Individual) : ImpactsOfIndividual?{
        return populations.values.find {
            it.any { i-> i.individual.sameActions(other) }
        }?.run {
            if (this.isEmpty())
                null
            else
                find{i -> i.individual.sameActions(other)}!!.impactInfo?.clone()
        }
    }

    /**
     * @return whether to skip targets for impact collections
     */
    fun skipTargetForImpactCollection(id : Int): Boolean{
        if (config.excludedTargetsForImpactCollection.isEmpty()) return false

        val local = IdMapper.isLocal(id)

        if (local){
            return config.excludedTargetsForImpactCollection.contains(LOCAL_OBJECTIVE_KEY)
        }

        return config.excludedTargetsForImpactCollection.any { idMapper.getDescriptiveId(id).startsWith(it) }
    }


    fun saveSnapshot(){
        if (!config.saveArchiveAfterMutation) return

        val index = time.evaluatedIndividuals
        val archiveContent = notCoveredTargets().filter { it >= 0 }.map { "$index,${it to getReachedTargetHeuristics(it)},${idMapper.getDescriptiveId(it)}" }

        val apath = Paths.get(config.archiveAfterMutationFile)
        if (apath.parent != null) Files.createDirectories(apath.parent)
        if (Files.notExists(apath)) Files.createFile(apath)

        if (archiveContent.isNotEmpty()) Files.write(apath, archiveContent, StandardOpenOption.APPEND)
    }

    /**
     * @return whether there exists any invalid evaluated individual in the population
     * note that it is useful for debugging
     */
    fun anyInvalidEvaluatedIndividual(): Boolean{
        return populations.values.any { e-> e.any { !it.isValid() } }
    }

//    fun chooseLatestImprovedTargets(size : Int) : Set<Int>{
//        return latestImprovement.asSequence().sortedByDescending { it.value }.toList().subList(0, min(size, latestImprovement.size)).map { it.key }.toSet()
//    }
//
//    fun chooseImproveTargetsAfter(index : Int) : Set<Int> = latestImprovement.filterValues { it >= index }.keys

    /**
     * this is only used for debugging purpose
     *
     * @return whether all genes in current populations are all locally valid
     */
    fun areAllPopulationGeneLocallyValid() : Boolean{
        return populations.values.flatten().all {
            it.individual.seeGenes().all { g-> g.isLocallyValid() }
        }
    }

}
