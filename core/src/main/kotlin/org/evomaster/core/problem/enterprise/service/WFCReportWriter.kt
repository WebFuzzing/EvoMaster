package org.evomaster.core.problem.enterprise.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.webfuzzing.commons.report.Faults
import com.webfuzzing.commons.report.ProblemDetails
import com.webfuzzing.commons.report.RESTReport
import org.evomaster.core.EMConfig
import org.evomaster.core.output.clustering.SplitResult
import org.evomaster.core.search.Solution
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class WFCReportWriter {


    @Inject
    private lateinit var config: EMConfig


    fun writeReport(solution: Solution<*>, splitResult: SplitResult) {

        val report = com.webfuzzing.commons.report.Report()

        report.schemaVersion = "0.0.1" //TODO
        report.toolName = "EvoMaster"
        report.toolVersion = this.javaClass.`package`?.implementationVersion ?: "unknown"
        report.creationTime = Date()
        report.totalTests = solution.individuals.size

        val faults = Faults()
        report.faults = faults
        //FIXME
        faults.totalNumber = solution.totalNumberOfDetectedFaults()

        report.problemDetails = ProblemDetails()

        if(config.problemType == EMConfig.ProblemType.REST) {
            val rest = RESTReport()
            report.problemDetails.rest = rest

            //TODO all other entries
        }
        //TODO other problem types

        val jackson = ObjectMapper()
        val json = jackson.writeValueAsString(report)

        val path = Paths.get(config.outputFolder, "report.json").toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(json)
    }
}