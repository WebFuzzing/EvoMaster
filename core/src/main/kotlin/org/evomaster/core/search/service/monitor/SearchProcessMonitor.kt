package org.evomaster.exps.monitor

import com.google.gson.GsonBuilder
import com.google.inject.Inject
import com.sun.org.apache.xpath.internal.operations.Bool
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.*
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.annotation.PostConstruct


/**
 * @decription: monitor 1) how are rest actions or rest individual selected regarding how to sampleAll
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
    var step :StepOfSearchProcess<*>? = null
    var isMutated : Boolean = false

    //record the progress of saved steps
    private var tb = 1

    private val evaluatedIndividuals : MutableList<EvaluatedIndividual<*>> = mutableListOf()

    companion object {
        //step is saved under the <process-data-folder>/data
        val DATA_FOLDER = "data"
        val FILE_TYPE = ".json"
        val NAME = "overall"

        val gson =GsonBuilder().registerTypeAdapter(RestAction::class.java, InterfaceAdapter<RestAction>())
                .registerTypeAdapter(Param::class.java, InterfaceAdapter<Param>())
                .registerTypeAdapter(Gene::class.java, InterfaceAdapter<Gene>())
                .create()
    }

    @PostConstruct
    fun postConstruct(){
        // TODO configure monitoring the details of search process
        if(config.enableProcessMonitor){
            time.addListener(this)
            initMonitorProcessOuputs()
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
            if(time.evaluatedActions > tb * config.maxActionEvaluations/config.processInterval){
                /*
                * step is assigned when an individual is evaluated (part of calculateCoverage of FitnessFunction),
                * but in order to record if the evaluated individual added into Archive, we need to save it after executing addIfNeeded in Archive
                * Since calculateCoverage is always followed by addIfNeed, the step should be not null.
                *
                * */

                step!!.added = added
                step!!.improvedArchive = improveArchive
                saveStep(step!!.indexOfEvaluation, step!!)
                tb++
            }
        }
    }


    private fun setOverall(){
        val stp = config.stoppingCriterion.toString()+"_"+
                (if(config.stoppingCriterion.toString().toLowerCase().contains("time")) config.maxTimeInSeconds.toString() else config.maxActionEvaluations)
        this.overall = SearchOverall(stp, time.evaluatedIndividuals, eval!!.individual, eval!!, archive, idMapper, time.getStartTime())
    }



    fun initMonitorProcessOuputs(){
        val path = Paths.get(config.processFiles)

        if(Files.exists(path)){
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach{
                        t -> run{
                        t.delete()
                    } }
        }
        if(config.showProgress) println("all files in ${path.toUri().toString()} are deleted")

    }
    fun saveOverall(){
        setOverall()
        writeByChannel(Paths.get(config.processFiles + File.separator + NAME  + FILE_TYPE), gson.toJson(this.overall))
    }

    fun saveStep(index:Int, v : StepOfSearchProcess<*>){
        writeByChannel(Paths.get(config.processFiles + File.separator+ DATA_FOLDER +File.separator + ""+ getInt(index) + FILE_TYPE), gson.toJson(v))
    }

    fun writeByChannel(path : Path, value :String){
        if (!Files.exists(path.parent)) Files.createDirectories(path.parent)
        Files.createFile(path)
        val buffer = ByteBuffer.wrap(value.toByteArray())
        FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).run {
            writeToChannel(this, buffer)
        }

    }

    @Throws(IOException::class)
    private fun writeToChannel(channel: FileChannel, buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
        channel.close()
    }


    fun getInt(value : Int) :String{
        return String.format("%0${config.maxActionEvaluations.toString().length}"+"d", value)
    }

}

