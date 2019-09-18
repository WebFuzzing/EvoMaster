package org.evomaster.experiments.stringMutation

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.evomaster.core.BaseModule
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.SearchTimeController

/**
 * created by manzh on 2019-09-16
 */
class Main {

    companion object{

        @JvmStatic
        fun main(args: Array<String>) {
            val n = 4
            println("=======archived-based===========")
            val archivedSolution = run(getArgs(), nTargets = n)

            println("=======standard===========")

            val defaultSolution = run(getArgs(probOfArchiveMutation = 0.0), nTargets = n)

        }


        private fun run(args: Array<String>, nTargets : Int = 1, specifiedLength : Int = 16, maxLength: Int = 16) : Solution<StringIndividual>{

            val injector: Injector = LifecycleInjector.builder()
                    .withModules(* arrayOf<Module>(StringModule(), BaseModule(args)))
                    .build().createInjector()

            val spd : StringProblemDefinition = injector.getInstance(StringProblemDefinition::class.java)
            spd.init(nTargets, specifiedLength, maxLength)
            spd.optima.forEachIndexed { index, s ->
                println("$index -> $s")
            }
            val mio = injector.getInstance(Key.get(
                    object : TypeLiteral<MioAlgorithm<StringIndividual>>() {}))
            val stc = injector.getInstance(SearchTimeController::class.java)
            val archive = injector.getInstance(Key.get(
                    object : TypeLiteral<Archive<StringIndividual>>() {}))

            val manager = injector.getInstance(LifecycleManager::class.java)
            manager.start()
            val solution = mio.search()
            println("Needed budget: ${stc.neededBudget()}")
            println("covered targets: ${solution.overall.coveredTargets()}")
            archive.reachedTargetHeuristics().forEach(::println)
            solution.individuals.forEachIndexed { index, evaluatedIndividual ->
                println("individual $index : ${evaluatedIndividual.individual.seeGenes().filterIsInstance<StringGene>().map { it.value }.joinToString(", ")}")
            }
            manager.close()

            return solution
        }

        private fun getArgs(budget: Int = 1000, probOfArchiveMutation : Double = 1.0, method : ImpactMutationSelection = ImpactMutationSelection.APPROACH_GOOD) = arrayOf(
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
                "focusedSearchActivationTime",
                (10.0/budget).toString()
        )
    }
}