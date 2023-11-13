package org.evomaster.core.search.service.monitor

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.output.TestSuiteFileName
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.*
import org.evomaster.core.utils.ReportWriter.writeByChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct


/**
 * monitor 1) how are rest actions or rest individual selected regarding how to sampleAll
 *         2) how does targets update
 */
class SearchProcessMonitor: SearchListener {

    @Inject
    private lateinit var time: SearchTimeController

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var archive: Archive<*>

    @Inject
    private lateinit var idMapper: IdMapper

    @Inject(optional = true)
    private lateinit var writer: TestSuiteWriter

    @Inject(optional = true)
    private var controller: RemoteController? = null

    private lateinit var overall : SearchOverall<*>

    private var controllerName : String? = null

    var eval: EvaluatedIndividual<*>? = null
    var step : StepOfSearchProcess<*>? = null
    var isMutated : Boolean = false

    /**
     * record the progress of saved steps
     * */
    private var tb = 1

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SearchProcessMonitor::class.java)

        /**
         * all steps of search are archived under the DATA_FOLDER, e.g., @see org.evomaster.core.EMConfig.processFiles/data
         * */
        private const val DATA_FOLDER = "data"

        /**
         * a name of a file to save final Archive, and it can be found in @see org.evomaster.core.EMConfig.processFiles/overall.json
         * */
        private const val NAME = "overall"

        /**
         * all steps and overall produced by a search monitor are saved as json files.
         * */
        private const val FILE_TYPE = ".json"

        private var gson : Gson? = null

        private val skippedClasses = listOf(
            StructuralElement::class.java.name,
            /*
                https://github.com/JetBrains/kotlin/blob/master/spec-docs/function-types.md
             */
            "kotlin.jvm.functions.Function1"
        )

        private val strategy: ExclusionStrategy = object : ExclusionStrategy {
            //TODO systematic way to configure the skipped field
            override fun shouldSkipField(field: FieldAttributes): Boolean {
                return field.name == "parent" || field.name == "bindingGenes"
            }

            //skip abstract StructuralElement element
            override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                return clazz!= null && skippedClasses.contains(clazz.name)
            }
        }

    }

    @PostConstruct
    fun postConstruct(){
        initMonitorProcessOutputs()
        if(config.enableProcessMonitor){
            time.addListener(this)
            if (config.processFormat == EMConfig.ProcessDataFormat.TEST_IND || config.processFormat == EMConfig.ProcessDataFormat.TARGET_TEST_IND){
                val dto = try {
                    controller?.getControllerInfo()
                }catch (e: Exception){
                    log.warn("Remote driver does not response with the exception message: ${e.cause!!.message}")
                    null
                }
                controllerName = dto?.fullName
            }
        }
    }

    override fun newActionEvaluated() {
        if(config.enableProcessMonitor && config.processFormat == EMConfig.ProcessDataFormat.JSON_ALL){
            step = StepOfSearchProcess(archive, time.evaluatedIndividuals, eval!!.individual, eval!!, System.currentTimeMillis(),isMutated)
        }

    }

    fun <T: Individual> record(added: Boolean, improveArchive : Boolean, evalInd : EvaluatedIndividual<T>){
        if(config.enableProcessMonitor){
            if(config.processInterval == 0.0 || time.percentageUsedBudget() >= tb * config.processInterval/100.0){
                when(config.processFormat){
                    EMConfig.ProcessDataFormat.JSON_ALL->{
                        if(evalInd != eval) throw IllegalStateException("Mismatched evaluated individual under monitor")
                        /*
                            step is assigned when an individual is evaluated (part of calculateCoverage of FitnessFunction),
                            but in order to record if the evaluated individual added into Archive, we need to save it after executing addIfNeeded in Archive
                            Since calculateCoverage is always followed by addIfNeed, the step should be not null.
                         */
                        step!!.added = added
                        step!!.improvedArchive = improveArchive
                        saveStep(step!!.indexOfEvaluation, step!!)
                        if(config.showProgress) log.info("number of targets: ${step!!.populations.size}")

                    }
                    EMConfig.ProcessDataFormat.TEST_IND , EMConfig.ProcessDataFormat.TARGET_TEST_IND->{
                        saveStepAsTest(index = time.evaluatedIndividuals,evalInd = evalInd, doesIncludeTarget = config.processFormat == EMConfig.ProcessDataFormat.TARGET_TEST_IND)
                    }
                }
                if(config.processInterval > 0.0) tb++
            }
        }
    }


    private fun setOverall(){
        val stp = config.stoppingCriterion.toString()+"_"+
                (if(config.stoppingCriterion.toString().toLowerCase().contains("time")) config.timeLimitInSeconds().toString() else config.maxActionEvaluations)
        this.overall = SearchOverall(stp, time.evaluatedIndividuals, eval!!.individual, eval!!, archive, idMapper, time.getStartTime())
    }



    private fun initMonitorProcessOutputs(){
        val path = Paths.get(config.processFiles)
        if(config.showProgress) log.info("Deleting all files in ${path.toUri()}")

        if(Files.exists(path)){
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach{
                        if(!it.delete())
                            log.warn("Fail to delete ${it.path}")
                    }
        }
    }
    fun saveOverall(){
        when(config.processFormat){
            EMConfig.ProcessDataFormat.JSON_ALL-> {
                setOverall()
                writeByChannel(
                        Paths.get(getOverallProcessAsPath()),
                        getGsonBuilder()?.toJson(this.overall)?:throw IllegalStateException("gson builder is null"))
            }
            else ->{}
        }

    }

    fun getOverallProcessAsPath() = "${config.processFiles}${File.separator}${getOverallFileName()}"

    fun getStepAsPath(index: Int, isTargetFile: Boolean=false) = "${getStepDirAsPath()}${File.separator}${getProcessFileName(getStepName(index, isTargetFile), isTargetFile)}"

    fun getStepDirAsPath() = "${config.processFiles}${File.separator}$DATA_FOLDER"

    private fun saveStep(index:Int, v : StepOfSearchProcess<*>){
        writeByChannel(
                Paths.get(getStepAsPath(index)),
                getGsonBuilder()?.toJson(v)?:throw java.lang.IllegalStateException("gson builder is null"))
    }

    private fun <T:Individual> saveStepAsTest(index: Int, evalInd: EvaluatedIndividual<T>, doesIncludeTarget : Boolean){
        val name = getStepName(index, false)
        val testFile = TestSuiteFileName(name)
        val solution = Solution(
            individuals = mutableListOf(evalInd),
            testSuiteNamePrefix = name,
            testSuiteNameSuffix = "",
            individualsDuringSeeding = listOf(),
            targetsDuringSeeding = listOf()
        )
        val content = writer.convertToCompilableTestCode(
                solution = solution,
                testSuiteFileName = testFile, controllerName = controllerName, controllerInput = null)
        writeByChannel(
                Paths.get(getStepAsPath(index)),
                content)
        if (doesIncludeTarget){
            val info = archive.exportCoveredTargetsAsPair(solution)
            writeByChannel(
                    Paths.get(getStepAsPath(index, true)),
                    info.map { it.first }.sorted().joinToString(System.lineSeparator()))
        }
    }

   private fun getStepName(value: Int, isTargetFile: Boolean): String {
       val num = String.format("%0${config.maxActionEvaluations.toString().length}d", value)
       return when(config.processFormat){
           EMConfig.ProcessDataFormat.JSON_ALL -> "EM_${num}Json"
           EMConfig.ProcessDataFormat.TEST_IND-> "EM_${num}Test"
           EMConfig.ProcessDataFormat.TARGET_TEST_IND-> "EM_${num}${if (isTargetFile) "Target" else "Test"}"
       }
   }

    fun getOverallFileName() : String{
        return NAME  + FILE_TYPE
    }

    private fun getProcessFileName(name : String, isTargetFile : Boolean = false) = when(config.processFormat){
        EMConfig.ProcessDataFormat.JSON_ALL-> "${name}.json"
        EMConfig.ProcessDataFormat.TEST_IND -> TestSuiteFileName(name).getAsPath(config.outputFormat)
        EMConfig.ProcessDataFormat.TARGET_TEST_IND -> {
            if (isTargetFile) "${name}.txt"
            else TestSuiteFileName(name).getAsPath(config.outputFormat)
        }
    }
    private fun getGsonBuilder() : Gson? {
        if (config.enableProcessMonitor && config.processFormat == EMConfig.ProcessDataFormat.JSON_ALL)
            if (gson == null) gson = GsonBuilder().registerTypeAdapter(RestCallAction::class.java, InterfaceAdapter<RestCallAction>())
                    .registerTypeAdapter(Param::class.java, InterfaceAdapter<Param>())
                    .registerTypeAdapter(Gene::class.java, InterfaceAdapter<Gene>())
                    .setExclusionStrategies(strategy)
                    .create()
        return gson
    }



}

