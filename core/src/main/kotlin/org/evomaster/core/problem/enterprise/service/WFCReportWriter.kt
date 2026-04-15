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
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
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

    private var lastTestFilePaths: Set<String> = emptySet()

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

        writeLowCodeIndex(prefix)
    }

    private fun writeLowCodeIndex(prefix: String) {
        val indexHtml = readResource("$prefix/index.html")
        val reportJs = readResource("$prefix/assets/report.js")
        val reportCss = readResource("$prefix/assets/report.css")
        val iconSvg = readResource("$prefix/assets/icon.svg")

        val reportJsonPath = Paths.get(config.outputFolder, "report.json").toAbsolutePath()
        val reportJson = if (Files.exists(reportJsonPath)) {
            reportJsonPath.toFile().readText(Charsets.UTF_8)
        } else {
            "{}"
        }

        val testFiles = readTestSourceFiles(lastTestFilePaths)

        val html = buildLowCodeHtml(indexHtml, reportJs, reportCss, iconSvg, reportJson, testFiles)

        val outPath = Paths.get(config.outputFolder, "low-code-index.html").toAbsolutePath()
        Files.createDirectories(outPath.parent)
        Files.deleteIfExists(outPath)
        Files.createFile(outPath)
        outPath.toFile().appendText(html)
    }

    private fun readTestSourceFiles(paths: Set<String>): Map<String, String> {
        val result = linkedMapOf<String, String>()
        // testSuitePath values in the WFC report are typically relative paths like
        // "org/bar/FooEM.kt" that the running web app resolves against the HTML base
        // (= the output folder). For the self-contained build we resolve the same way:
        // absolute paths stay as-is, relative paths are resolved under config.outputFolder.
        val base = Paths.get(config.outputFolder).toAbsolutePath()
        for (p in paths) {
            try {
                val candidate = Paths.get(p)
                val resolved = if (candidate.isAbsolute) candidate else base.resolve(p)
                if (Files.exists(resolved)) {
                    result[p] = resolved.toFile().readText(Charsets.UTF_8)
                }
            } catch (_: Exception) {
                // best-effort: the UI already tolerates missing test file content
            }
        }
        return result
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
        lastTestFilePaths = report.testFilePaths
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

    companion object {

        /**
         * Pure function: takes the raw index.html and the bundled webreport assets,
         * and produces a single self-contained "low-code" HTML document. The output:
         *   - drops the external /assets/report.js and /assets/report.css references
         *   - inlines them as <style> and <script type="module"> elements
         *   - inlines the SVG favicon as a data URI
         *   - prepends a bootstrap <script> that sets window.__WFC_LOW_CODE__ = true
         *     and installs a window.fetch shim returning embedded report.json / test
         *     sources, so the page works via file:// without an HTTP server.
         */
        internal fun buildLowCodeHtml(
            indexHtml: String,
            reportJs: String,
            reportCss: String,
            iconSvg: String,
            reportJson: String,
            testFiles: Map<String, String>
        ): String {
            require(!reportCss.contains("</style", ignoreCase = true)) {
                "report.css contains </style> which cannot be safely inlined into a <style> element"
            }

            val doc = Jsoup.parse(indexHtml)
            doc.outputSettings().prettyPrint(false)

            // Inline the favicon as a data URI so it does not hit the filesystem.
            doc.select("link[rel=icon]").forEach {
                val base64 = Base64.getEncoder().encodeToString(iconSvg.toByteArray(Charsets.UTF_8))
                it.attr("href", "data:image/svg+xml;base64,$base64")
            }

            // Drop the external bundle references; we will inline them below.
            doc.select("script[src*=report.js]").remove()
            doc.select("link[href*=report.css]").remove()

            val embedded = linkedMapOf<String, String>()
            embedded["./report.json"] = reportJson
            testFiles.forEach { (k, v) -> embedded[k] = v }

            // JSON is valid JS literal. Escape </script and </style occurrences so the
            // embedded payload cannot prematurely close the host <script> element
            // (\/ is a valid JSON escape for /, decoded back to the original string).
            val embeddedJson = ObjectMapper().writeValueAsString(embedded)
                .replace("</script", "<\\/script", ignoreCase = true)
                .replace("</style", "<\\/style", ignoreCase = true)

            val bootstrap = """
                window.__WFC_LOW_CODE__ = true;
                window.__WFC_EMBEDDED__ = $embeddedJson;
                (function(origFetch){
                  window.fetch = function(url){
                    var key = typeof url === 'string' ? url : (url && url.url);
                    if (window.__WFC_EMBEDDED__ && Object.prototype.hasOwnProperty.call(window.__WFC_EMBEDDED__, key)) {
                      var body = window.__WFC_EMBEDDED__[key];
                      var headers = { 'Content-Type': key.indexOf('.json') >= 0 ? 'application/json' : 'text/plain' };
                      return Promise.resolve(new Response(body, { status: 200, headers: headers }));
                    }
                    return origFetch.apply(this, arguments);
                  };
                })(window.fetch);
            """.trimIndent()

            val head = doc.head()

            val bootstrapEl = head.appendElement("script")
            bootstrapEl.appendChild(DataNode(bootstrap))

            val styleEl = head.appendElement("style")
            styleEl.appendChild(DataNode(reportCss))

            // Same escape for the bundle itself — \/ is a valid JS escape inside string
            // and regex literals, so decoded semantics are preserved.
            val safeJs = reportJs.replace("</script", "<\\/script", ignoreCase = true)
            val scriptEl = doc.body().appendElement("script")
            scriptEl.attr("type", "module")
            scriptEl.appendChild(DataNode(safeJs))

            return doc.outerHtml()
        }
    }
}