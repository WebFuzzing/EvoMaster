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
 * @author: manzhang
 * @date: 31/08/2018
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

    private var cs = ""
    private var tb = 1

    private val evaluatedIndividuals : MutableList<EvaluatedIndividual<*>> = mutableListOf()

    companion object {
        var doesSave = true
        val DATA_FOLDER = "data"
        val gson =GsonBuilder().registerTypeAdapter(RestAction::class.java, InterfaceAdapter<RestAction>())
                .registerTypeAdapter(Param::class.java, InterfaceAdapter<Param>())
                .registerTypeAdapter(Gene::class.java, InterfaceAdapter<Gene>())
                .create()

        //FIXME need to change accordingly
        var digit = 7

        fun getFileType() : String{
            return ".json"
        }

        fun getInt(digit:Int, value : Int) :String{
            return String.format("%0$digit"+"d", value)
        }

        fun covertInt(digit: Int, value : String) : Int{
            var s = value.indexOfFirst { it -> it.toInt() != 0 }
            return value.substring(s).toInt()
        }

        fun getOverallName() : String{
            return "overall"
        }

//        fun generateAlgoFolder(algo : String, stoppingCriterion: String, sampling: String, budget: Int, maxTestSize: Int, population : Int,  indexOfRun : Int= -1): String{
//            return algo + "_"+stoppingCriterion+"_"+sampling+"_"+budget + "_"+ maxTestSize+ (if(population != 30) "_P"+population.toString() else "")+ (if(indexOfRun != -1) "_R"+indexOfRun.toString() else "")
//        }

//        fun generateAlgoFolder (_config : EMConfig, indexOfRun: Int): String{
//            return generateAlgoFolder (_config.algorithm.name, _config.stoppingCriterion.toString(), _config.smartSampling.toString(),
//                    (if(_config.stoppingCriterion.toString().toLowerCase().contains("time")) _config.maxTimeInSeconds else _config.maxActionEvaluations), _config.maxTestSize, _config.populationSize, indexOfRun)
//
//        }
    }

    @PostConstruct
    fun postConstruct(){
        // TODO configure monitoring the details of search process
        if(config.enableProcessMonitor){
            time.addListener(this)

            setCS(config.statisticsColumnId)
            initMonitorProcessOuputs()
        }
    }

    override fun newActionEvaluated() {
        if(config.enableProcessMonitor){
            evaluatedIndividuals.add(eval!!)
            step = StepOfSearchProcess(archive, time.evaluatedIndividuals, eval!!.individual, eval!!, System.currentTimeMillis(),isMutated)
        }

    }

    //TODO Man
    fun record(added: Boolean, improveArchive : Boolean, evalInd : EvaluatedIndividual<*>){
        if(config.enableProcessMonitor){
            if(evalInd != eval) throw IllegalStateException("Mismatched evaluated individual under monitor")
            if(doesSave){
                if(time.evaluatedActions > tb * 100){
                    step!!.added = added
                    step!!.improvedArchive = improveArchive
                    saveStep(step!!.indexOfEvaluation, step!!)
                    println(step!!.populations.size)
                    tb++
                }
            }
        }
    }


    private fun setOverall(){
        var stp = config.stoppingCriterion.toString()+"_"+
                (if(config.stoppingCriterion.toString().toLowerCase().contains("time")) config.maxTimeInSeconds.toString() else config.maxActionEvaluations)
        this.overall = SearchOverall(stp, time.evaluatedIndividuals, eval!!.individual, eval!!, archive, idMapper, time.getStartTime())
    }

    fun setCS(name : String){
        this.cs = name
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
        var overalltp = Paths.get(config.processFiles + File.separator + getOverallName()  + getFileType())
        writeByChannel(overalltp, gson.toJson(this.overall))
    }

    fun saveStep(index:Int, v : StepOfSearchProcess<*>){
        var tp = Paths.get(config.processFiles + File.separator+ DATA_FOLDER +File.separator + ""+if(digit < 1) index else getInt(digit, index) + getFileType())
        writeByChannel(tp, gson.toJson(v))
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


}

