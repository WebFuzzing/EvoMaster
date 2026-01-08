package org.evomaster.core.mutationweight

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.mutationweight.GeneWeightTestSchema
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.MutationWeightControl

/**
 * created by manzh on 2020-06-02
 */

class CompareAvgNumberToMutate {

    private val config: EMConfig
    private val time : SearchTimeController
    private val apc: AdaptiveParameterControl
    private val mwc: MutationWeightControl
    private val utest = MannWhitneyUTest()

    private val ds = arrayOf(0.25, 0.5, 0.75)

    init {
        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(BaseModule()))
                .build().createInjector()

        config = injector.getInstance(EMConfig::class.java)
        time = injector.getInstance(SearchTimeController::class.java)
        apc = injector.getInstance(AdaptiveParameterControl::class.java)
        mwc = injector.getInstance(MutationWeightControl::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
        config.focusedSearchActivationTime = 0.5
        config.maxEvaluations = 10
    }

    fun run(){
        time.newActionEvaluation(5)

        val ind_sql3_rest2 = GeneWeightTestSchema.newRestIndividual(name = "GET:/gw/foo/{key}", numSQLAction = 1)

        val ind_sql15_rest2 = GeneWeightTestSchema.newRestIndividual(name = "GET:/gw/foo/{key}", numSQLAction = 5)

        val ind_sql15_rest12 = GeneWeightTestSchema.newRestIndividual(name = "GET:/gw/foo/{key}", numSQLAction = 5, numRestAction = 6)

        val candidates = listOf(ind_sql3_rest2, ind_sql15_rest2, ind_sql15_rest12)


        arrayOf(false, true).forEach { e->
            println(">>>>>>>>>>>>>>>> enableNumOfGroup: $e")
            candidates.forEach { ind->
                ds.forEach { d->
                    compareAvgT(d, ind, enableNumOfGroup = e)
                }
            }
        }


    }
    private fun compareAvgT(d : Double, individual: RestIndividual, sampleSize: Int = 30000, enableNumOfGroup: Boolean){

        config.d = d

        val all = individual.seeTopGenes().filter { it.isMutable() }
        val rest = individual.seeTopGenes(ActionFilter.NO_SQL).filter { it.isMutable() }
        val sql = individual.seeTopGenes(ActionFilter.ONLY_SQL).filter { it.isMutable() }

        val s11 = mutableListOf<Double>()
        val s12 = mutableListOf<Double>()

        collectSelectedGenes(s11, s12, all, sql, rest, sampleSize, enableNumOfGroup)

        val p1 = utest.mannWhitneyUTest(s11.toDoubleArray(), s12.toDoubleArray())
        val ration_avg = (0 until sampleSize).map { s11[it]/s12[it] }.sum()/sampleSize
        val ratio_sum = s11.sum() / s12.sum()

        println("========= d:$d; all:${all.size}; sql:${sql.size}; rest:${rest.size} =============")
        println("p-value: ${if(p1 < 0.01) "<0.01" else if(p1 <0.05)  "<0.05" else p1}")
        println("avg(s1): ${s11.sum()/sampleSize}; avg(s2): ${s12.sum()/sampleSize}")
        println("avg(s1/s2): $ration_avg; sum(s1)/sum(s2): $ratio_sum")

    }


    private fun collectSelectedGenes(specialSQL : MutableList<Double>, general : MutableList<Double>, all: List<Gene>, sql: List<Gene>, rest: List<Gene>, sampleSize : Int, enableNumOfGroup : Boolean) {

        val tmp = mutableListOf<Gene>()

        (0 until sampleSize).forEach { _ ->

            //simulate special handling SQL
            while (tmp.isEmpty()){
                tmp.addAll(mwc.selectSubGene(rest, false, numOfGroup = if (enableNumOfGroup) 2 else 1))
                tmp.addAll(mwc.selectSubGene(sql, false, numOfGroup = if (enableNumOfGroup) 2 else 1))
            }
            specialSQL.add(tmp.size.toDouble())
            tmp.clear()

            general.add(mwc.selectSubGene(all, false, forceNotEmpty = true).size.toDouble())
        }
    }
}

fun main(args: Array<String>) {
    CompareAvgNumberToMutate().run()
}