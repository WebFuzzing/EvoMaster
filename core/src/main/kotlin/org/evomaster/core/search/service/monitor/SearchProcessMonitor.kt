package org.evomaster.core.search.service.monitor

import com.google.gson.GsonBuilder
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.annotation.PostConstruct


/**
 * @description: monitor 1) how are rest actions or rest individual selected regarding how to sampleAll
 *                      2) how does targets update
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

    private lateinit var overall : SearchOverall<*>

    var eval: EvaluatedIndividual<*>? = null
    var step : StepOfSearchProcess<*>? = null
    var isMutated : Boolean = false

    /**
     * record the progress of saved steps
     * */
    private var tb = 1

    private val evaluatedIndividuals : MutableList<EvaluatedIndividual<*>> = mutableListOf()

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SearchProcessMonitor::class.java)

        /**
         * all steps of search are archived under the DATA_FOLDER, e.g., @see org.evomaster.core.EMConfig.processFiles/data
         * */
        const val DATA_FOLDER = "data"

        /**
         * a name of a file to save final Archive, and it can be found in @see org.evomaster.core.EMConfig.processFiles/overall.json
         * */
        const val NAME = "overall"

        /**
         * all steps and overall produced by a search monitor are saved as json files.
         * */
        const val FILE_TYPE = ".json"

        private val gson =GsonBuilder().registerTypeAdapter(RestAction::class.java, InterfaceAdapter<RestAction>())
                .registerTypeAdapter(Param::class.java, InterfaceAdapter<Param>())
                .registerTypeAdapter(Gene::class.java, InterfaceAdapter<Gene>())
                .create()
    }

    @PostConstruct
    fun postConstruct(){
        initMonitorProcessOutputs()
        if(config.enableProcessMonitor){
            time.addListener(this)
        }
    }

    override fun newActionEvaluated() {
        if(config.enableProcessMonitor){
            evaluatedIndividuals.add(eval!!)
            step = StepOfSearchProcess(archive, time.evaluatedIndividuals, eval!!.individual, eval!!, System.currentTimeMillis(),isMutated)
        }

    }

    fun record(added: Boolean, improveArchive : Boolean, evalInd : EvaluatedIndividual<*>){
        if(config.enableProcessMonitor){
            if(evalInd != eval) throw IllegalStateException("Mismatched evaluated individual under monitor")
            if(time.evaluatedActions >= tb * config.maxActionEvaluations/config.processInterval){
                /*
                * step is assigned when an individual is evaluated (part of calculateCoverage of FitnessFunction),
                * but in order to record if the evaluated individual added into Archive, we need to save it after executing addIfNeeded in Archive
                * Since calculateCoverage is always followed by addIfNeed, the step should be not null.
                *
                * */

                step!!.added = added
                step!!.improvedArchive = improveArchive
                saveStep(step!!.indexOfEvaluation, step!!)
                if(config.showProgress) log.info("number of targets: ${step!!.populations.size}")
                tb++
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
        setOverall()
        writeByChannel(Paths.get(config.processFiles + File.separator + getOverallFileName()), gson.toJson(this.overall))
    }

    private fun saveStep(index:Int, v : StepOfSearchProcess<*>){
        writeByChannel(Paths.get(config.processFiles + File.separator+ DATA_FOLDER +File.separator + ""+ getStepFileName(index) ), gson.toJson(v))
    }


    private fun writeByChannel(path : Path, value :String){
        if (!Files.exists(path.parent)) Files.createDirectories(path.parent)
        Files.createFile(path)
        val buffer = ByteBuffer.wrap(value.toByteArray())
        FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).run {
            writeToChannel(this, buffer)
        }

    }

    private fun writeToChannel(channel: FileChannel, buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
        channel.close()
    }

    fun getStepFileName(value : Int) :String{
        return String.format("%0${config.maxActionEvaluations.toString().length}"+"d", value) + FILE_TYPE
    }

    fun getOverallFileName() : String{
        return NAME  + FILE_TYPE
    }

}

