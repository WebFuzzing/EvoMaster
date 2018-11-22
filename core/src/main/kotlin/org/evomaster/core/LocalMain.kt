package org.evomaster.core

import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.search.algorithms.*

class LocalMain {
    companion object {
        fun getArgs(algo : String,
                    cs : String,
                    enableProcessMonitor : Boolean = false,
                    run : Int = 1,
                    probOfSmartSampling : Double = 0.5,
                    smartSampling : String = EMConfig.SmartSamplingCriterion.DEPENDENCE.toString(),
                    maxTestSize : Int = 10,
                    isStoppedByActions : Boolean = true,
                    budget: Int = 10000,
                    population: Int = 30,
                    port : Int = 40100,
                    baseFolder : String = "/Users/mazh001/Documents/Workspace/temp-results"

        ): Array<String> {

            val label = arrayOf(algo, smartSampling, probOfSmartSampling.toString(), maxTestSize, budget, "R"+run.toString()).joinToString("_")
            return arrayOf(
                    "--stoppingCriterion", if(isStoppedByActions) "FITNESS_EVALUATIONS" else "TIME",
                    if(isStoppedByActions) "--maxActionEvaluations" else "--maxTimeInSeconds", ""+budget,
                    "--sutControllerPort",""+ port,
                    "--statisticsColumnId", cs,
                    "--seed",run.toLong().toString(),
                    "--outputFolder", baseFolder + "/$cs/$label/tests",
                    "--algorithm",algo,
                    "--populationSize",population.toString(),
                    "--enableProcessMonitor",enableProcessMonitor.toString(),
                    "--processFiles", baseFolder + "/$cs/$label/process",
                    "--probOfSmartSampling", probOfSmartSampling.toString(),
                    "--smartSampling",smartSampling,
                    "--writeStatistics",true.toString(),
                    "--snapshotInterval", "1",
                    "--statisticsFile",baseFolder + "/$cs/$label/reports/statistics.csv",
                    "--snapshotStatisticsFile",baseFolder + "/$cs/$label/reports/snapshot.csv",
                    "--problemType",if(smartSampling == EMConfig.SmartSamplingCriterion.DEPENDENCE.toString()) "RESTII" else "REST",
                    "--maxTestSize", maxTestSize.toString() //dynamically control a size of test during a search

            );
        }
    }
}

fun main(args : Array<String>){
    Main.main(LocalMain.getArgs("SAMPLE", "NCS", true, 71))
}