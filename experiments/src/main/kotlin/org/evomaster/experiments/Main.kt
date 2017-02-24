package org.evomaster.experiments

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.MosaAlgorithm
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.algorithms.WtsAlgorithm
import org.evomaster.core.search.service.SearchAlgorithm
import org.evomaster.experiments.maxv.LinearIndividual
import org.evomaster.experiments.maxv.LinearModule
import org.evomaster.experiments.maxv.LinearProblemDefinition
import java.util.*


class Main {

    companion object {

        /**
         * Main entry point of the EvoMaster application
         */
        @JvmStatic
        fun main(args: Array<String>) {

            println("algorithm,coverage,n,range,budget")

            val budget = 1000
            val range = 1000
            val disruptiveP = 0.01

            val repetitions = 20

            for(seed in 0 until repetitions){
                for(nTargets in listOf(1,2,3,4,5,10,20,30,40,50,60,70,80,90,100)){
//                for(nTargets in listOf(1,2,3,4,5)){

                    val optima = createOptima(nTargets, range, seed.toLong())

                    run(seed.toLong(), budget, nTargets, range, disruptiveP, optima)
                }
            }

        }

        fun run(seed: Long, budget: Int, nTargets: Int, range: Int, disruptiveP: Double, optima: List<Int>){
            val mio = getMio(seed, budget, nTargets, range, disruptiveP, optima)
            val rand = getRand(seed, budget, nTargets, range, disruptiveP, optima)
            val mosa = getMosa(seed, budget, nTargets, range, disruptiveP, optima)
            val wts = getWts(seed, budget, nTargets, range, disruptiveP, optima)

            val algs: List<Pair<Injector, SearchAlgorithm<LinearIndividual>>> = listOf(
                    mosa, rand, wts, mio
            )

            algs.forEach { a ->

                val manager = a.first.getInstance(LifecycleManager::class.java)

                manager.start()
                val sol = a.second.search()
                manager.close()

                val covered = sol.overall.coveredTargets()
                val cov = 100.0 * ( covered.toDouble() / nTargets.toDouble())

                println("${a.second.getType()},$cov,$nTargets,$range,$budget")
            }
        }


        fun createOptima(n: Int, range: Int, seed: Long) : List<Int>{
            val optima = mutableListOf<Int>()
            val rand = Random(seed)
            (1..n).forEach {
                optima.add(rand.nextInt(range+1))
            }

            return optima
        }


        fun getMio(seed: Long, budget: Int, nTargets: Int, range: Int, disruptiveP: Double, optima: List<Int>)
                : Pair<Injector, SearchAlgorithm<LinearIndividual>>{

            val injector = getInjector(seed, budget, nTargets, range, disruptiveP, optima)
            val config = injector.getInstance(EMConfig::class.java)
            config.algorithm = EMConfig.Algorithm.MIO

            val alg = injector.getInstance(Key.get(
                    object : TypeLiteral<MioAlgorithm<LinearIndividual>>() {}))

            return Pair(injector, alg)
        }

        fun getRand(seed: Long, budget: Int, nTargets: Int, range: Int, disruptiveP: Double, optima: List<Int>)
                : Pair<Injector, SearchAlgorithm<LinearIndividual>>{

            val injector = getInjector(seed, budget, nTargets, range, disruptiveP, optima)
            val config = injector.getInstance(EMConfig::class.java)
            config.algorithm = EMConfig.Algorithm.RANDOM

            val alg = injector.getInstance(Key.get(
                    object : TypeLiteral<RandomAlgorithm<LinearIndividual>>() {}))

            return Pair(injector, alg)
        }

        fun getMosa(seed: Long, budget: Int, nTargets: Int, range: Int, disruptiveP: Double, optima: List<Int>)
                : Pair<Injector, SearchAlgorithm<LinearIndividual>>{

            val injector = getInjector(seed, budget, nTargets, range, disruptiveP, optima)
            val config = injector.getInstance(EMConfig::class.java)
            config.algorithm = EMConfig.Algorithm.MOSA

            val alg = injector.getInstance(Key.get(
                    object : TypeLiteral<MosaAlgorithm<LinearIndividual>>() {}))
            return Pair(injector, alg)
        }

        fun getWts(seed: Long, budget: Int, nTargets: Int, range: Int, disruptiveP: Double, optima: List<Int>)
                : Pair<Injector, SearchAlgorithm<LinearIndividual>>{

            val injector = getInjector(seed, budget, nTargets, range, disruptiveP, optima)
            val config = injector.getInstance(EMConfig::class.java)
            config.algorithm = EMConfig.Algorithm.WTS

            val alg = injector.getInstance(Key.get(
                    object : TypeLiteral<WtsAlgorithm<LinearIndividual>>() {}))
            return Pair(injector, alg)
        }

        fun getInjector(seed: Long, budget: Int, nTargets: Int, range: Int, disruptiveP: Double, optima: List<Int>) : Injector{

            val injector: Injector = LifecycleInjector.builder()
                    .withModules(* arrayOf<Module>(LinearModule(), BaseModule()))
                    .build().createInjector()

            val config = injector.getInstance(EMConfig::class.java)
            config.seed = seed
            config.maxFitnessEvaluations = budget
            config.populationSize = 50 //as in MOSA paper
            config.tournamentSize = 10 //as in MOSA paper

            val lpd = injector.getInstance(LinearProblemDefinition::class.java)
            lpd.nTargets = nTargets
            lpd.disruptiveP = disruptiveP
            lpd.range = range
            lpd.optima.clear()
            lpd.optima.addAll(optima)

            return injector
        }
    }
}