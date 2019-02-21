package org.evomaster.core

class LocalMain {
    companion object {
        fun getArgs(algo : String,
                    cs : String,
                    enableProcessMonitor : Boolean = false,
                    run : Int = 1,
                    smartSampling : String = EMConfig.SmartSamplingStrategy.RESOURCES.toString(),
                    sampleControl : String = EMConfig.ResourceSamplingControl.Actions.toString(),
                    probOfSmartSampling : Double = 0.5,
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
//                    "--probOfRandomSampling","1.0",
//                    "--endProbOfRandomSampling","1.0",
                    "--smartSampling",smartSampling,
                    "--sampleControl", sampleControl,
                    "--writeStatistics",true.toString(),
                    "--snapshotInterval", "1",
                    "--statisticsFile",baseFolder + "/$cs/$label/reports/statistics.csv",
                    "--snapshotStatisticsFile",baseFolder + "/$cs/$label/reports/snapshot.csv",
                    "--problemType","REST",
                    //"--showProgress", true.toString(),
                    "--maxTestSize", maxTestSize.toString() //dynamically control a size of test during a search

            );
        }
    }
}

fun main(args : Array<String>){
    Main.main(LocalMain.getArgs("MIO", "proxyprint", true, 10002))
}