package org.evomaster.core.problem.enterprise.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.webfuzzing.commons.report.*
import org.evomaster.core.EMConfig
import org.evomaster.core.output.TestCaseCode
import org.evomaster.core.output.TestSuiteCode
import org.evomaster.core.problem.enterprise.EnterpriseActionResult
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.Statistics
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class WFCReportWriter {


    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var statistics: Statistics

    @Inject
    private lateinit var sampler: Sampler<*>

    private fun getTestId(suite: TestSuiteCode, test: TestCaseCode) =
        suite.testSuitePath + "#" + test.name



    fun writeWebApp(){
        val prefix = "/webreport"
        exportResource(prefix, "/index.html")
        exportResource(prefix, "/robots.txt")
        exportResource(prefix, "/webreport.py")
        exportResource(prefix, "/webreport.command", true)
        exportResource(prefix, "/webreport.bat", true)
        exportResource(prefix, "/assets/icon.svg")
        exportResource(prefix, "/assets/report.js")
        exportResource(prefix, "/assets/report.css")
    }

    private fun exportResource(prefix: String, resource: String, executable: Boolean = false) {

        val text = readResource(prefix+resource)

        val path = Paths.get(config.outputFolder, resource).toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        val file = path.toFile()
        file.appendText(text)
        file.setExecutable(executable)
    }

    private fun readResource(path: String) : String {

      return  WFCReportWriter::class.java.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalArgumentException("Resource not found: $path")
    }

    private fun getWFCVersion() : String{
        val pomPropertiesPath = "META-INF/maven/com.webfuzzing/commons/pom.properties"
        return WFCReportWriter::class.java.classLoader.getResourceAsStream(pomPropertiesPath)
            ?.use { stream ->
                Properties().apply { load(stream) }
                    .getProperty("version")
                    ?.let { return it }

            }
            ?: "not specified"
    }

    fun writeReport(solution: Solution<*>, suites: List<TestSuiteCode>) {

        val data = statistics.getData(solution)

        val report = Report()
        val toolName = "EvoMaster"

        report.schemaVersion = getWFCVersion()
        report.toolName = toolName
        report.toolVersion = this.javaClass.`package`?.implementationVersion ?: "snapshot"
        report.creationTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        report.totalTests = solution.individuals.size
        report.testFilePaths = suites.map { it.testSuitePath }.toSet()
        report.executionTimeInSeconds = getElement(data, Statistics.ELAPSED_SECONDS).toInt()

        val faults = Faults()
        report.faults = faults
        faults.totalNumber = solution.totalNumberOfDetectedFaults()
        for(suite in suites){
            for(test in suite.tests){

                val testId = getTestId(suite, test)
                for(ea in test.evaluatedIndividual.evaluatedMainActions()){
                    val faultIds = (ea.result as EnterpriseActionResult)
                        .getFaults()
                        .map { FaultCategoryId().apply { code = it.category.code; context = it.context} }

                    if(faultIds.isNotEmpty()) {
                        val ff = FoundFault()
                        ff.testCaseId = testId
                        ff.operationId = ea.action.getName()
                        ff.faultCategories = faultIds.toSet()
                        faults.foundFaults.add(ff)
                    }
                }

                val tc = TestCase()
                tc.id = testId
                tc.name = test.name
                tc.filePath = suite.testSuitePath
                tc.startLine = test.startLine
                tc.endLine = test.endLine
                report.testCases.add(tc)
            }
        }

        report.problemDetails = ProblemDetails()

        if(config.problemType == EMConfig.ProblemType.REST) {
            val rest = RESTReport()
            report.problemDetails.rest = rest

            rest.outputHttpCalls = solution.individuals.sumOf { it.individual.size() }
            //TODO make sure that auth is counted here if making calls... i am quite sure it is currently not :(
            //TODO might need to collect auth counter separately, and then added here, or maybe not?
            rest.evaluatedHttpCalls = getElement(data, Statistics.EVALUATED_ACTIONS).toInt()
            rest.endpointIds = sampler.getActionDefinitions().map { it.getName() }.toSet()

            for(suite in suites) {
                for (test in suite.tests) {
                    val testId = getTestId(suite, test)

                    val eas = test.evaluatedIndividual.evaluatedMainActions()
                    val statusMap = eas.groupBy( { it.action.getName() }, { (it.result as RestCallResult).getStatusCode()})

                    val ces = statusMap.entries
                        .map {
                            val ce = CoveredEndpoint()
                            ce.testCaseId = testId
                            ce.endpointId = it.key
                            ce.httpStatus = it.value.toSet()
                            ce
                        }
                    rest.coveredHttpStatus.addAll(ces)
                }
            }
        }
        //TODO other problem types

        if(!config.blackBox){
            val coverage = Coverage()
            coverage.toolName = toolName

            val lines = CoverageCriterion()
            lines.name = "Line Coverage"
            lines.covered = getElement(data,Statistics.COVERED_LINES ).toInt()
            lines.total = getElement(data, Statistics.TOTAL_LINES ).toInt()
            coverage.criteria.add(lines)

            val branches = CoverageCriterion()
            branches.name = "Branch Coverage"
            branches.covered = getElement(data, Statistics.COVERED_BRANCHES).toInt()
            branches.total = getElement(data, Statistics.TOTAL_BRANCHES).toInt()
            coverage.criteria.add(branches)

            report.extra.add(coverage)
        }


        val jackson = ObjectMapper()
        val json = jackson.writeValueAsString(report)

        val path = Paths.get(config.outputFolder, "report.json").toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(json)
    }

    private fun getElement(data:  List<Statistics.Pair>, headerName: String):  String{
        return data.first{ it.header == headerName }.element
    }
}