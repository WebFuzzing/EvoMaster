package org.evomaster.experiments.archiveMutation.stringProblem

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.service.Statistics
import org.evomaster.experiments.archiveMutation.ArchiveProblemType
import org.evomaster.experiments.archiveMutation.stringProblem.allDepDynamic.ADepDynamicStringProblemDefinition
import org.evomaster.experiments.archiveMutation.stringProblem.allDepDynamic.ADepDynamicStringModule
import org.evomaster.experiments.archiveMutation.stringProblem.allIndepStable.IndepStableStringModule
import org.evomaster.experiments.archiveMutation.stringProblem.allIndepStable.IndepStableStringProblemDefinition
import org.evomaster.experiments.archiveMutation.stringProblem.partialDepDynamic.PDepDynamicStringModule
import org.evomaster.experiments.archiveMutation.stringProblem.partialDepDynamic.PDepDynamicStringProblemDefinition
import org.evomaster.experiments.archiveMutation.stringProblem.partialIndepStable.PIndepStableStringModule
import org.evomaster.experiments.archiveMutation.stringProblem.partialIndepStable.PIndepStableStringProblemDefinition
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * created by manzh on 2019-09-16
 */
class Main {

    companion object{

        @JvmStatic
        fun main(args: Array<String>) {
            //default(args)
            val folder = "/Users/mazh001/Documents/GitHub/postdoc_hk/2019/03-archive-based-mutation-mio/arc_exp_results_focusLatest/"
            allSetting(folder, listOf(1), 1, listOf(1000), listOf(ImpactMutationSelection.NONE), listOf(1.0), includeNone = false)
        }

        private fun default(args: Array<String>){
            val baseFolder = if (args.size == 1) args.first() else "/Users/mazh001/Documents/GitHub/postdoc_hk/2019/03-archive-based-mutation-mio/arc_exp_results/"
            val targets = listOf(1, 10, 50, 100)
            val runs = 30
            val budgets = listOf(1000, 10000, 100000)
            val impactSelection = ImpactMutationSelection.values().filter { it != ImpactMutationSelection.NONE }.sorted()
            val probOfArchiveMutations = listOf(0.5, 1.0)
            allSetting(baseFolder, targets, runs, budgets, impactSelection, probOfArchiveMutations)
        }

        private fun allSetting(baseFolder: String, targets : List<Int>, runs :Int, budgets : List<Int>, impactSelection : List<ImpactMutationSelection>, probOfArchiveMutations : List<Double>, includeNone : Boolean = true){
            if (!Files.exists(Paths.get(baseFolder))) Files.createDirectories(Paths.get(baseFolder))
            val path = Paths.get("${baseFolder}summary.txt")
            if (!Files.exists(path)) Files.createFile(path)
            val specifiedLength = 16
            val formatMaxLength = 28

            targets.forEach { n->
                budgets.forEach { budget->
                    val head = mutableListOf("Runs, Budgets($runs, $budget)")
                    if (includeNone){
                        head.add("${ImpactMutationSelection.NONE.name}[false]")
                        head.add("${ImpactMutationSelection.NONE.name}[true]")
                    }

                    probOfArchiveMutations.forEach { p->
                        impactSelection.forEach { s->
                            head.add("${s.name}[$p]")
                        }
                    }
                    val headStr = head.map { s -> formatString(formatMaxLength, s) }.joinToString("\t")
                    Files.write(path, ( headStr + System.lineSeparator()).toByteArray(), StandardOpenOption.APPEND)
                    println(headStr)
                    ArchiveProblemType.values().forEach { pt->
                        arrayOf(false, true).forEach {fs->
                            val results = Array( (if (includeNone) 2 else 0) + impactSelection.size * probOfArchiveMutations.size){0}
                            val item = "$pt($n, $fs)"
                            (0 until runs).forEach { i ->
                                if (includeNone){
                                    val defaultSolution =
                                            run(getArgs(budget = budget, method = ImpactMutationSelection.NONE, probOfArchiveMutation = 0.0, problem = pt, n = n, run = i, writeStatistics = true, disableStructureMutationDuringFocusSearch = fs, baseFolder = baseFolder), nTargets = n, specifiedLength = specifiedLength, problem = pt)
                                    results[0] += defaultSolution.overall.coveredTargets()

                                    val defaultSolution1 =
                                            run(getArgs(budget = budget, method = ImpactMutationSelection.NONE, probOfArchiveMutation = 1.0, problem = pt, n = n, run = i, writeStatistics = true, disableStructureMutationDuringFocusSearch = fs, baseFolder = baseFolder), nTargets = n, specifiedLength = specifiedLength, problem = pt)
                                    results[1] += defaultSolution1.overall.coveredTargets()
                                }
                                var index = if (includeNone) 2 else 0
                                probOfArchiveMutations.forEach { p->
                                    impactSelection.forEach {selection->
                                        val archivedSolution = run(getArgs(budget = budget, method = selection, probOfArchiveMutation = p,  problem = pt, n = n,run = i, writeStatistics = true, disableStructureMutationDuringFocusSearch = fs, baseFolder = baseFolder), nTargets = n, specifiedLength = specifiedLength, problem = pt)
                                        results[index] += archivedSolution.overall.coveredTargets()
                                        index++
                                    }
                                }

                            }
                            val result = listOf(item).plus(results.map { format(it/runs.toDouble())}).map { s -> formatString(formatMaxLength, s) }.joinToString("\t")
                            Files.write(path, (result+System.lineSeparator()).toByteArray(), StandardOpenOption.APPEND)
                            println(result)
                        }
                    }
                    Files.write(path, (System.lineSeparator()).toByteArray(), StandardOpenOption.APPEND)
                }
            }

        }
        private fun format(value : Double) = "%.2f".format(value)
        private fun formatString(len : Int, value :String) = "%${len}s".format(value)


        private fun run(args: Array<String>, nTargets : Int = 1, specifiedLength : Int = 16, maxLength: Int = 16, problem: ArchiveProblemType) : Solution<StringIndividual>{

            val modules = when(problem){
                ArchiveProblemType.ALL_DEP_DYNAMIC -> arrayOf<Module>(ADepDynamicStringModule(),StringModule(), BaseModule(args))
                ArchiveProblemType.PAR_DEP_DYNAMIC -> arrayOf<Module>(PDepDynamicStringModule(),StringModule(), BaseModule(args))
                ArchiveProblemType.ALL_IND_STABLE -> arrayOf<Module>(IndepStableStringModule(), StringModule(), BaseModule(args))
                ArchiveProblemType.PAR_IND_STABLE -> arrayOf<Module>(PIndepStableStringModule(), StringModule(), BaseModule(args))
            }

            val injector: Injector = LifecycleInjector.builder()
                    .withModules(* modules)
                    .build().createInjector()

            when(problem){
                ArchiveProblemType.ALL_DEP_DYNAMIC -> injector.getInstance(ADepDynamicStringProblemDefinition::class.java).init(nTargets, specifiedLength, maxLength)
                ArchiveProblemType.PAR_DEP_DYNAMIC -> injector.getInstance(PDepDynamicStringProblemDefinition::class.java).init(nTargets, specifiedLength, maxLength)
                ArchiveProblemType.ALL_IND_STABLE -> injector.getInstance(IndepStableStringProblemDefinition::class.java).init(nTargets, specifiedLength, maxLength)
                ArchiveProblemType.PAR_IND_STABLE -> injector.getInstance(PIndepStableStringProblemDefinition::class.java).init(nTargets, specifiedLength, maxLength)
            }


            val mio = injector.getInstance(Key.get(
                    object : TypeLiteral<MioAlgorithm<StringIndividual>>() {}))
            //val stc = injector.getInstance(SearchTimeController::class.java)

            val config = injector.getInstance(EMConfig::class.java)

            val statistics = injector.getInstance(Statistics::class.java)

            val manager = injector.getInstance(LifecycleManager::class.java)
            manager.start()
            val solution = mio.search()
            //usedBudgets.add(stc.lastActionImprovement/stc.evaluatedActions.toDouble())

            if (config.writeStatistics){
                statistics.writeStatistics(solution)
            }
            manager.close()
            return solution
        }

        private fun getArgs(
                budget: Int = 100000,
                probOfArchiveMutation : Double = 1.0,
                method : ImpactMutationSelection = ImpactMutationSelection.FEED_BACK,
                n: Int,
                problem: ArchiveProblemType,
                run : Int,
                writeStatistics : Boolean = true,
                disableStructureMutationDuringFocusSearch : Boolean,
                baseFolder : String
        ) = arrayOf(
                "--stoppingCriterion",
                "FITNESS_EVALUATIONS",
                "--maxActionEvaluations",
                budget.toString(),
                "--enableTrackIndividual",
                "false",
                "--enableTrackEvaluatedIndividual",
                "true",
                "--showProgress",
                "false",
                "--probOfArchiveMutation",
                probOfArchiveMutation.toString(),
                "--geneSelectionMethod",
                method.toString(),
                "--maxTestSize",
                "1",
                "--createTests",
                false.toString(),
                "--writeStatistics",
                writeStatistics.toString(),
                "--appendToStatisticsFile",
                "true",
                "--snapshotInterval",
                if (writeStatistics) 10.toString() else (-1).toString(),
                "--statisticsFile",
                "${baseFolder}R${run}_T${n}_${problem.name}_${if (probOfArchiveMutation>0.0)"archive_statistics.csv" else "standard_statistics.csv"}",
                "--snapshotStatisticsFile",
                "${baseFolder}R${run}_T${n}_${problem.name}_${if (probOfArchiveMutation>0.0)"archive_snapshot.csv" else "standard_snapshot.csv"}",
                "--enableArchiveGeneMutation",
                if (probOfArchiveMutation > 0.0) true.toString() else false.toString(),
                "--disableStructureMutationDuringFocusSearch",
                disableStructureMutationDuringFocusSearch.toString(),
                "--statisticsColumnId",
                "$n-${problem.name}"

        )
    }
}