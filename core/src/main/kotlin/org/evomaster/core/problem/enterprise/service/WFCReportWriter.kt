package org.evomaster.core.problem.enterprise.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.webfuzzing.commons.report.FaultCategoryId
import com.webfuzzing.commons.report.Faults
import com.webfuzzing.commons.report.FoundFault
import com.webfuzzing.commons.report.ProblemDetails
import com.webfuzzing.commons.report.RESTReport
import com.webfuzzing.commons.report.TestCase
import org.evomaster.core.EMConfig
import org.evomaster.core.output.TestCaseCode
import org.evomaster.core.output.TestSuiteCode
import org.evomaster.core.output.clustering.SplitResult
import org.evomaster.core.problem.enterprise.EnterpriseActionResult
import org.evomaster.core.search.Solution
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class WFCReportWriter {


    @Inject
    private lateinit var config: EMConfig

    private fun getTestId(suite: TestSuiteCode, test: TestCaseCode) =
        suite.testSuitePath + "#" + test.name

    fun writeReport(solution: Solution<*>, suites: List<TestSuiteCode>) {

        val report = com.webfuzzing.commons.report.Report()

        report.schemaVersion = "0.0.1" //TODO
        report.toolName = "EvoMaster"
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
                    val ff = FoundFault()
                    ff.testCaseId = testId
                    ff.operationId = ea.action.getName()
                    ff.faultCategories = faultIds.toSet()
                    faults.foundFaults.add(ff)
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