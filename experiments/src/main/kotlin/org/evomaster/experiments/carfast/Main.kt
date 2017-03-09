package org.evomaster.experiments.carfast

import com.foo.artifacts.carfast.TP0
import com.foo.artifacts.carfast.Triangle
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
import org.evomaster.core.search.algorithms.MosaAlgorithm
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.algorithms.WtsAlgorithm
import org.evomaster.core.search.service.Statistics


class Main {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            //base(Triangle::class.java.name)
            base(TP0::class.java.name)
        }

        fun base(className: String){

            val args = arrayOf(
                    "--createTests", "false",
                    "--seed", "-1",
                    "--maxFitnessEvaluations", "10000",
                    "--stoppingCriterion", "FITNESS_EVALUATIONS",
                    "--writeStatistics", "true",
                    "--appendToStatisticsFile", "true",
                    "--structureMutationProbability", "0.01"
            )

            val algs = listOf("MIO","MOSA","WTS","RANDOM")

            (0 until 30).forEach {
                algs.forEach { a ->
                    run(className, args.plus("--algorithm").plus(a).plus("--statisticsColumnId").plus(className))
                }
            }
        }

        fun run(className:String, args: Array<String>){//: Solution<ISFIndividual> {

            val injector: Injector = LifecycleInjector.builder()
                    .withModules(* arrayOf<Module>(
                            BaseModule(args),
                            ISFModule(className)
                    ))
                    .build()
                    .createInjector()

            val config = injector.getInstance(EMConfig::class.java)

            val key = when(config.algorithm) {
                EMConfig.Algorithm.MIO -> Key.get(
                        object : TypeLiteral<MioAlgorithm<ISFIndividual>>() {})
                EMConfig.Algorithm.RANDOM -> Key.get(
                        object : TypeLiteral<RandomAlgorithm<ISFIndividual>>() {})
                EMConfig.Algorithm.WTS -> Key.get(
                        object : TypeLiteral<WtsAlgorithm<ISFIndividual>>() {})
                EMConfig.Algorithm.MOSA -> Key.get(
                        object : TypeLiteral<MosaAlgorithm<ISFIndividual>>() {})
                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }

            val imp = injector.getInstance(key)
            val manager = injector.getInstance(LifecycleManager::class.java)

            manager.start()
            val solution = imp.search()
            manager.close()

            val statistics = injector.getInstance(Statistics::class.java)
            statistics.writeStatistics(solution)
        }
    }
}