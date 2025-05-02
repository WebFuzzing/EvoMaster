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

    fun writeReport(solution: Solution<*>, suites: List<TestSuiteCode>) {

        val report = com.webfuzzing.commons.report.Report()
        val toolName = "EvoMaster"

        report.schemaVersion = "0.0.1" //TODO
        report.toolName = toolName
        report.toolVersion = this.javaClass.`package`?.implementationVersion ?: "unknown"
        report.creationTime = Date()
        report.totalTests = solution.individuals.size
        report.testFilePaths = suites.map { it.testSuitePath }.toSet()


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

            rest.totalHttpCalls = solution.individuals.sumOf { it.individual.size() }
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
            val data = statistics.getData(solution)
            val coverage = Coverage()
            coverage.toolName = toolName

            val lines = CoverageCriterion()
            lines.name = "Line Coverage"
            lines.covered = data.first{ it.header == Statistics.COVERED_LINES }.element.toInt()
            lines.total = data.first{ it.header == Statistics.TOTAL_LINES }.element.toInt()
            coverage.criteria.add(lines)

            val branches = CoverageCriterion()
            branches.name = "Branch Coverage"
            branches.covered = data.first{ it.header == Statistics.COVERED_BRANCHES }.element.toInt()
            branches.total = data.first{ it.header == Statistics.TOTAL_BRANCHES }.element.toInt()
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
}