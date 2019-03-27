package org.evomaster.core

class LocalMain {
    companion object {
        fun getArgs(algo : String,
                    cs : String,
                    run : Int = 1,
                    smartSampling : String = EMConfig.SmartSamplingStrategy.RESOURCES.toString(),
                    sampleControl : String = EMConfig.ResourceSamplingControl.ConArchive.toString(),
                    probOfSmartSampling : Double = 0.5,
                    maxTestSize : Int = 10,
                    isStoppedByActions : Boolean = true,
                    budget: Int = 100000,
                    baseFolder : String = "/Users/mazh001/Documents/Workspace/temp-results"

        ): Array<String> {

            val label = arrayOf(algo, smartSampling, probOfSmartSampling.toString(), maxTestSize, budget, "R"+run.toString()).joinToString("_")
            return arrayOf(
                    "--stoppingCriterion", if(isStoppedByActions) "FITNESS_EVALUATIONS" else "TIME",
                    if(isStoppedByActions) "--maxActionEvaluations" else "--maxTimeInSeconds", ""+budget,
                    "--statisticsColumnId", cs,
                    "--seed",run.toLong().toString(),
                    "--outputFolder", baseFolder + "/$cs/$label/tests",
                    "--algorithm",algo,
                    "--enableProcessMonitor",false.toString(),
                    "--processFiles", baseFolder + "/$cs/$label/process",

                    //resource-based sampling
                    "--probOfSmartSampling", probOfSmartSampling.toString(),
                    "--smartSamplingStrategy",smartSampling,
                    "--sampleControl", sampleControl,
                    "--probOfEnablingResourceDependencyHeuristics", 0.0.toString(),


                    //archive-based mutation
                    "--probOfArchiveMutation", "0.0",
                    //track
                    "--enableTrackEvaluatedIndividual", false.toString(),

                    //allowDataFromDB
                    "--allowDataFromDB", true.toString(),
                    "--probOfSelectFromDB", "0.1",

                    //disable db
                    "--heuristicsForSQL", false.toString(),
                    "--generateSqlDataWithDSE",false.toString(),
                    "--generateSqlDataWithSearch", false.toString(),

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
    Main.main(LocalMain.getArgs("MIO", "scout-api",10002))
}