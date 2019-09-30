package org.evomaster.experiments.archiveMutation.stringProblem

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.service.Statistics
import org.evomaster.core.search.service.mutator.geneMutation.CharPool
import org.evomaster.experiments.archiveMutation.ArchiveProblemType
import org.evomaster.experiments.archiveMutation.stringProblem.allDepDynamic.ADepDynamicStringModule
import org.evomaster.experiments.archiveMutation.stringProblem.allIndepStable.IndepStableStringModule
import org.evomaster.experiments.archiveMutation.stringProblem.partialDepDynamic.PDepDynamicStringModule
import org.evomaster.experiments.archiveMutation.stringProblem.partialIndepStable.PIndepStableStringModule
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime

/**
 * created by manzh on 2019-09-16
 */
class Main {

    companion object{

        private val targets = listOf(4)
        private val runs = 30
        private val budgets = listOf(3000)
        private val impactSelection = ImpactMutationSelection.values().filter { it != ImpactMutationSelection.NONE }.sorted()
        private val adaptiveGeneSelections = EMConfig.AdaptiveSelection.values().toList().sorted()
        private val archiveGeneMutation = EMConfig.ArchiveGeneMutation.values().toList().sorted()
        private val probOfArchiveMutations = listOf(0.25, 0.5, 0.75, 1.0)
        private val focusSearch = arrayOf(false)
        private val problems = ArchiveProblemType.values().toList()

        @JvmStatic
        fun main(args: Array<String>) {
            val folder = if (args.size == 1) args.first() else "/Users/mazh001/Documents/GitHub/postdoc_hk/2019/03-archive-based-mutation-mio/arc_exp_results_dynamic_advanced_adaptive_selection/"
            adaptiveSelection(folder)
            //default(arrayOf(folder))
        }
        /**
         * exp for impact selection
         */
        private fun adaptiveSelection(baseFolder: String){
            val problems = listOf(ArchiveProblemType.PAR_IND_STABLE, ArchiveProblemType.PAR_DEP_DYNAMIC)
            val rateOfImpacts = listOf(0.2, 0.5, 0.8, 1.0) //0.2, 0.5, 0.8, 1.0
            val configs = produceConfigs(
                    impactSelection = impactSelection,
                    probOfArchiveMutations = probOfArchiveMutations,
                    archiveGeneMutation = archiveGeneMutation,
                    adaptiveGeneSelections = adaptiveGeneSelections,
                    includeNone = true,
                    baseLineWithMutation = true,
                    focusSearch = focusSearch
            )

            if (!Files.exists(Paths.get(baseFolder))) Files.createDirectories(Paths.get(baseFolder))
            val path = Paths.get("${baseFolder}summary.txt")
            if (!Files.exists(path)) Files.createFile(path)
            val specifiedLength = 5

            Files.write(path, "=========${LocalDateTime.now()}===== length of string is $specifiedLength========${System.lineSeparator()}".toByteArray(), StandardOpenOption.APPEND)
            targets.forEach { n->
                budgets.forEach { budget->
                    rateOfImpacts.forEach { rp->
                        problems.forEach { pt->
                            val header = "--------${pt.name}($rp): $n targets, $runs runs, $budget fitness evaluations-------"
                            Files.write(path, "$header${System.lineSeparator()}".toByteArray(), StandardOpenOption.APPEND)
                            println(header)
                            configs.forEach { config->
                                println(config.getName())
                                var total = 0
                                (0 until runs).forEach { i ->
                                    val solution = run(
                                            getArgs(
                                                    budget = budget,
                                                    n = n,
                                                    run = i,
                                                    problem = pt,
                                                    baseFolder = baseFolder,
                                                    writeStatistics = false,
                                                    config = config

                                            ),
                                            n,
                                            specifiedLength,
                                            problem = pt,
                                            rateOfImpact = rp,
                                            charPool = CharPool.WORD)
                                    //println(solution.overall.coveredTargets())
                                    total += solution.overall.coveredTargets()
                                }
                                val row = "${config.getName()},${format(total/runs.toDouble())}${System.lineSeparator()}"
                                Files.write(path, row.toByteArray(), StandardOpenOption.APPEND)
                            }
                        }
                    }
                }
            }
        }


        /**
         * exp for string gene mutation
         */
        private fun default(args: Array<String>){
            val baseFolder = if (args.size == 1) args.first() else "/Users/mazh001/Documents/GitHub/postdoc_hk/2019/03-archive-based-mutation-mio/arc_exp_results/"

            allSetting(
                    baseFolder,
                    targets,
                    runs,
                    budgets,
                    impactSelection,
                    probOfArchiveMutations,
                    true,
                    focusSearch,
                    problems = problems,
                    archiveGeneMutation = archiveGeneMutation,
                    adaptiveGeneSelections = adaptiveGeneSelections)
        }

        private fun allSetting(
                baseFolder: String,
                targets : List<Int>,
                runs :Int,
                budgets : List<Int>,
                impactSelection : List<ImpactMutationSelection>,
                probOfArchiveMutations : List<Double>,
                includeNone : Boolean = true,
                focusSearch : Array<Boolean>,
                problems: List<ArchiveProblemType>,
                archiveGeneMutation : List<EMConfig.ArchiveGeneMutation>,
                adaptiveGeneSelections : List<EMConfig.AdaptiveSelection>){

            if (!Files.exists(Paths.get(baseFolder))) Files.createDirectories(Paths.get(baseFolder))
            val path = Paths.get("${baseFolder}summary.txt")
            if (!Files.exists(path)) Files.createFile(path)
            val specifiedLength = 16

            val configs = produceConfigs(
                    impactSelection = impactSelection,
                    probOfArchiveMutations = probOfArchiveMutations,
                    archiveGeneMutation = archiveGeneMutation,
                    adaptiveGeneSelections = adaptiveGeneSelections,
                    includeNone = includeNone,
                    focusSearch = focusSearch
            )
            Files.write(path, "=========${LocalDateTime.now()}=============${System.lineSeparator()}".toByteArray(), StandardOpenOption.APPEND)
            targets.forEach { n->
                budgets.forEach { budget->
                    problems.forEach { pt->
                        val head = "--------${pt.name}-------"
                        Files.write(path, "$head${System.lineSeparator()}".toByteArray(), StandardOpenOption.APPEND)
                        configs.forEach { config->
                            println("$head ${config.getName()}")
                            var total = 0
                            (0 until runs).forEach { i ->
                                val solution = run(
                                        getArgs(
                                                budget = budget,
                                                n = n,
                                                run = i,
                                                problem = pt,
                                                baseFolder = baseFolder,
                                                writeStatistics = true,
                                                config = config

                                        ),
                                        n,
                                        specifiedLength,
                                        problem = pt,
                                        rateOfImpact = 0.0)
                                println(solution.overall.coveredTargets())
                                total += solution.overall.coveredTargets()
                            }
                            val row = "${config.getName()},${format(total/runs.toDouble())}${System.lineSeparator()}"
                            Files.write(path, row.toByteArray(), StandardOpenOption.APPEND)
                            //Files.write(path, (System.lineSeparator()).toByteArray(), StandardOpenOption.APPEND)
                        }
                    }

                }
            }

        }
        private fun format(value : Double) = "%.2f".format(value)
        private fun formatString(len : Int, value :String) = if (value.length > len) value else "%${len}s".format(value)


        private fun run(args: Array<String>, nTargets : Int = 1, specifiedLength : Int = 16, maxLength: Int = 16, problem: ArchiveProblemType, rateOfImpact : Double, charPool: CharPool = CharPool.ALL) : Solution<StringIndividual>{

            val specialized = when(problem){
                ArchiveProblemType.ALL_DEP_DYNAMIC -> ADepDynamicStringModule(nTargets, specifiedLength, maxLength, 1.0)
                ArchiveProblemType.PAR_DEP_DYNAMIC -> PDepDynamicStringModule(nTargets, specifiedLength, maxLength, if (rateOfImpact <= 0) 0.4 else rateOfImpact)
                ArchiveProblemType.ALL_IND_STABLE -> IndepStableStringModule(nTargets, specifiedLength, maxLength, 1.0)
                ArchiveProblemType.PAR_IND_STABLE -> PIndepStableStringModule(nTargets, specifiedLength, maxLength, if (rateOfImpact <= 0) 0.4 else rateOfImpact, charPool)
            }


            val injector: Injector = LifecycleInjector.builder()
                    .withModules(* arrayOf(specialized,StringModule(),BaseModule(args)))
                    .build().createInjector()


            val mio = injector.getInstance(Key.get(
                    object : TypeLiteral<MioAlgorithm<StringIndividual>>() {}))
            //val stc = injector.getInstance(SearchTimeController::class.java)

            //val config = injector.getInstance(EMConfig::class.java)

            //val statistics = injector.getInstance(Statistics::class.java)

            val manager = injector.getInstance(LifecycleManager::class.java)
            manager.start()
            val solution = mio.search()
            //usedBudgets.add(stc.lastActionImprovement/stc.evaluatedActions.toDouble())
//            solution.individuals.forEach { i->
//                i.individual.seeGenes().filterIsInstance<StringGene>().map { g-> "${g.name}:${g.mutatedtimes}-${g.resetTimes}-${g.resetTimes/g.mutatedtimes.toDouble()}"}.forEach(::println)
//            }
//            if (config.writeStatistics){
//                statistics.writeStatistics(solution)
//            }
            manager.close()
            return solution
        }
        private fun getArgs(
                budget : Int = 100000, n: Int, run : Int, writeStatistics : Boolean = true, baseFolder: String, problem: ArchiveProblemType, config: ExpConfig
        ) = getArgs(
                budget=budget,
                n=n,
                run = run,
                writeStatistics = writeStatistics,
                baseFolder = baseFolder,
                problem = problem,
                disableStructureMutationDuringFocusSearch = config.disableStructureMutationDuringFocusSearch,
                probOfArchiveMutation = config.probOfArchiveMutation,
                method = config.method,
                archiveGeneMutation = config.archiveGeneMutation,
                adaptiveGeneSelection = config.adaptiveGeneSelection

        )

        private fun getArgs(
                budget: Int = 100000,
                n: Int,
                problem: ArchiveProblemType,
                run : Int,
                writeStatistics : Boolean = true,
                baseFolder : String,
                disableStructureMutationDuringFocusSearch : Boolean,
                probOfArchiveMutation : Double = 1.0,
                method : ImpactMutationSelection = ImpactMutationSelection.NONE,
                archiveGeneMutation : EMConfig.ArchiveGeneMutation,
                adaptiveGeneSelection : EMConfig.AdaptiveSelection
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
                "--archiveGeneMutation",
                archiveGeneMutation.toString(),
                "--disableStructureMutationDuringFocusSearch",
                disableStructureMutationDuringFocusSearch.toString(),
                "--adaptiveGeneSelection",
                adaptiveGeneSelection.toString(),
                "--statisticsColumnId",
                "$n-${problem.name}",
                "--heuristicsForSQL",
                false.toString(),
                "--extractSqlExecutionInfo",
                false.toString(),
                "--generateSqlDataWithSearch",
                false.toString()

        )

        fun produceConfigs(impactSelection : List<ImpactMutationSelection>,
                           probOfArchiveMutations : List<Double>,
                           includeNone : Boolean = true,
                           baseLineWithMutation : Boolean = true,
                           focusSearch : Array<Boolean>,
                           archiveGeneMutation : List<EMConfig.ArchiveGeneMutation>,
                           adaptiveGeneSelections : List<EMConfig.AdaptiveSelection>) : List<ExpConfig>{
            val configs = mutableListOf<ExpConfig>()

            if (includeNone){
                configs.add(
                        ExpConfig(
                                probOfArchiveMutation = 0.0,
                                method = ImpactMutationSelection.NONE,
                                disableStructureMutationDuringFocusSearch = false,
                                adaptiveGeneSelection = EMConfig.AdaptiveSelection.FIXED_SELECTION,
                                archiveGeneMutation = EMConfig.ArchiveGeneMutation.NONE
                        )
                )

               if (baseLineWithMutation){
                   configs.add(
                           ExpConfig(
                                   probOfArchiveMutation = 1.0,
                                   method = ImpactMutationSelection.NONE,
                                   disableStructureMutationDuringFocusSearch = false,
                                   adaptiveGeneSelection = EMConfig.AdaptiveSelection.FIXED_SELECTION,
                                   archiveGeneMutation = EMConfig.ArchiveGeneMutation.SPECIFIED
                           )
                   )

                   configs.add(
                           ExpConfig(
                                   probOfArchiveMutation = 1.0,
                                   method = ImpactMutationSelection.NONE,
                                   disableStructureMutationDuringFocusSearch = false,
                                   adaptiveGeneSelection = EMConfig.AdaptiveSelection.FIXED_SELECTION,
                                   archiveGeneMutation = EMConfig.ArchiveGeneMutation.ADAPTIVE
                           )
                   )
               }
            }

            focusSearch.forEach { fs->
                probOfArchiveMutations.forEach { p->
                    adaptiveGeneSelections.forEach {aSelection->
                        when(aSelection){
                            EMConfig.AdaptiveSelection.FIXED_SELECTION -> {
                                impactSelection.forEach {selection->
                                    archiveGeneMutation.forEach {gMutation->
                                        configs.add(ExpConfig(
                                                probOfArchiveMutation = p,
                                                method = selection,
                                                disableStructureMutationDuringFocusSearch = fs,
                                                adaptiveGeneSelection = aSelection,
                                                archiveGeneMutation = gMutation
                                        ))
                                    }
                                }
                            }else->{
                            archiveGeneMutation.forEach {gMutation->
                                configs.add(ExpConfig(
                                        probOfArchiveMutation = p,
                                        method = ImpactMutationSelection.NONE,
                                        disableStructureMutationDuringFocusSearch = fs,
                                        adaptiveGeneSelection = aSelection,
                                        archiveGeneMutation = gMutation
                                ))
                            }
                         }
                        }
                    }
                }
            }

            return configs
        }

        data class ExpConfig(
                val probOfArchiveMutation : Double ,
                val method : ImpactMutationSelection ,
                val disableStructureMutationDuringFocusSearch : Boolean,
                val archiveGeneMutation : EMConfig.ArchiveGeneMutation,
                val adaptiveGeneSelection : EMConfig.AdaptiveSelection
        ){
            fun getName() = "$probOfArchiveMutation-$adaptiveGeneSelection[$method]-$archiveGeneMutation"
        }
    }
}