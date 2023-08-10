package org.evomaster.core.problem.api.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.enterprise.service.EnterpriseFitness
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.ws.rs.core.Response

/**
 * abstract class for handling fitness of API based SUT, such as REST, GraphQL, RPC
 */
abstract class ApiWsFitness<T> : EnterpriseFitness<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ApiWsFitness::class.java)
        const val DEFAULT_FAULT_CODE = "framework_code"
    }





    @Inject
    protected lateinit var writer: TestSuiteWriter

    @Inject
    protected lateinit var sampler: Sampler<T>

    lateinit var infoDto: SutInfoDto




    /**
     * In general, we should avoid having SUT send close requests on the TCP socket.
     * However, Tomcat (default in SpringBoot) by default will do that any 100 requests... :(
     */
    protected fun handlePossibleConnectionClose(response: Response) {
        if(response.getHeaderString("Connection")?.contains("close", true) == true){
            searchTimeController.reportConnectionCloseRequest(response.status)
        }
    }


    override fun targetsToEvaluate(targets: Set<Int>, individual: T): Set<Int> {
        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list
        val ts = targets.filter { !IdMapper.isLocal(it) }.toMutableSet()
        val nc = archive.notCoveredTargets().filter { !IdMapper.isLocal(it) && !ts.contains(it)}
        recordExceededTarget(nc)
        return when {
            ts.size > 100 -> randomness.choose(ts, 100)
            nc.isEmpty() -> ts
            else -> ts.plus(randomness.choose(nc, 100 - ts.size))
        }
    }

    private fun recordExceededTarget(targets: Collection<Int>){
        if(!config.recordExceededTargets) return
        if (targets.size <= 100) return

        val path = Paths.get(config.exceedTargetsFile)
        if (Files.notExists(path.parent)) Files.createDirectories(path.parent)
        if (Files.notExists(path)) Files.createFile(path)
        Files.write(path, listOf(time.evaluatedIndividuals.toString()).plus(targets.map { idMapper.getDescriptiveId(it) }), StandardOpenOption.APPEND)
    }






}