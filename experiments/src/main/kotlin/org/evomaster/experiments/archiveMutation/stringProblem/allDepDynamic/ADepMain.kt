package org.evomaster.experiments.archiveMutation.stringProblem.allDepDynamic

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
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.Statistics
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringModule

/**
 * created by manzh on 2019-09-16
 */
class ADepMain {

    companion object{

        @JvmStatic
        fun main(args: Array<String>) {
            val n = 1
            val specifiedLength = 16
            val problem = "$n-$specifiedLength"
            println("=======archived-based===========")
            var archive_total = 0
            var standard_total = 0
            val times = 100
            (0 until times).forEach {
                val archivedSolution = run(getArgs(problem = problem), nTargets = n, specifiedLength = specifiedLength)
                archive_total += archivedSolution.overall.coveredTargets()
            }
            println(archive_total/times.toDouble())
            println("=======standard===========")
            (0 until times).forEach {
                val defaultSolution = run(getArgs(probOfArchiveMutation = 0.0, problem = problem), nTargets = n, specifiedLength = specifiedLength)
                standard_total += defaultSolution.overall.coveredTargets()
            }
            println(standard_total/times.toDouble())
        }


        private fun run(args: Array<String>, nTargets : Int = 1, specifiedLength : Int = 16, maxLength: Int = 16) : Solution<StringIndividual>{

            val injector: Injector = LifecycleInjector.builder()
                    .withModules(* arrayOf<Module>(ADepDynamicStringModule(), StringModule(), BaseModule(args)))
                    .build().createInjector()

            val spd : ADepDynamicStringProblemDefinition = injector.getInstance(ADepDynamicStringProblemDefinition::class.java)
            spd.init(nTargets, specifiedLength, maxLength)

            val mio = injector.getInstance(Key.get(
                    object : TypeLiteral<MioAlgorithm<StringIndividual>>() {}))
            val stc = injector.getInstance(SearchTimeController::class.java)
            val archive = injector.getInstance(Key.get(
                    object : TypeLiteral<Archive<StringIndividual>>() {}))
            val config = injector.getInstance(EMConfig::class.java)

            val statistics = injector.getInstance(Statistics::class.java)


            val manager = injector.getInstance(LifecycleManager::class.java)
            manager.start()
            val solution = mio.search()
//            println("Needed budget: ${stc.neededBudget()}")
//            println("covered targets: ${solution.overall.coveredTargets()}")
//            archive.reachedTargetHeuristics().forEach(::println)
//            solution.individuals.forEachIndexed { index, evaluatedIndividual ->
//                println("individual $index : ${evaluatedIndividual.individual.seeGenes().filterIsInstance<StringGene>().map { it.value }.joinToString(", ")}")
//            }
            if (config.writeStatistics){
                statistics.writeStatistics(solution)
            }

            manager.close()

            return solution
        }

        private fun getArgs(budget: Int = 1000, probOfArchiveMutation : Double = 1.0, method : ImpactMutationSelection = ImpactMutationSelection.NONE, problem: String, writeStatistics : Boolean = false) = arrayOf(
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
                "--createTests",
                false.toString(),
                "--enableArchiveGeneMutation",
                if (probOfArchiveMutation>0.0)true.toString() else false.toString()
        )
    }
}