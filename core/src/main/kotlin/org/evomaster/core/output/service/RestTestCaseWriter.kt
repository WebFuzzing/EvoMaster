package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.SqlWriter
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.enterprise.EnterpriseActionResult
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.link.RestLinkParameter
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.utils.StringUtils
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*

class RestTestCaseWriter : HttpWsTestCaseWriter {

    companion object {
        private val log = LoggerFactory.getLogger(RestTestCaseWriter::class.java)
    }

    @Inject
    private lateinit var partialOracles: PartialOracles

    @Inject
    private lateinit var callGraphService: CallGraphService


    constructor() : super()

    /**
     * ONLY for tests
     */
    constructor(config: EMConfig, partialOracles: PartialOracles) : super() {
        this.config = config
        this.partialOracles = partialOracles
    }


    /**
     * When we make a HTTP call, do we need to store the response in a variable for following HTTP calls
     * or to create assertions on it?
     */
    override fun needsResponseVariable(call: HttpWsAction, res: HttpWsCallResult): Boolean {

        val k = call as RestCallAction

        return super.needsResponseVariable(call, res)
                //|| (config.expectationsActive && partialOracles.generatesExpectation(call as RestCallAction, res))
                || (!res.stopping && k.saveCreatedResourceLocation)
                || (!res.stopping && k.hasFollowedBackwardLink())
    }

    override fun handleTestInitialization(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testName: String
    ) {
        super.handleTestInitialization(lines, baseUrlOfSut, ind, insertionVars,testName)
    }

    override fun handleActionCalls(
            lines: Lines,
            baseUrlOfSut: String,
            ind: EvaluatedIndividual<*>,
            insertionVars: MutableList<Pair<String, String>>,
            testCaseName: String,
            testSuitePath: Path?
    ) {
        //SQL actions are generated in between
        if (ind.individual is RestIndividual) {
            if((ind.individual as RestIndividual).getResourceCalls().isNotEmpty()){
                ind.evaluatedResourceActions().forEachIndexed { index, c ->
                    // db
                    if (c.first.isNotEmpty())
                        SqlWriter.handleDbInitialization(
                            format,
                            c.first,
                            lines,
                            ind.individual.seeSqlDbActions(),
                            groupIndex = index.toString(),
                            insertionVars = insertionVars,
                            skipFailure = config.skipFailureSQLInTestFile
                        )
                    //actions
                    c.second.forEach { a ->
                        val exeuctionIndex = ind.individual.seeMainExecutableActions().indexOf(a.action)
                        handleSingleCall(a, exeuctionIndex, ind.fitness, lines, testCaseName, testSuitePath, baseUrlOfSut)
                    }
                }
            }else{
                ind.evaluatedMainActions().forEachIndexed { index, evaluatedAction ->
                    handleSingleCall(evaluatedAction, index, ind.fitness, lines, testCaseName, testSuitePath, baseUrlOfSut)
                }
            }

        }
    }

    protected fun locationVar(id: String): String {
        /*
            Ids are supposed to be unique, but might have invalid characters for a variable
         */
        val suffix = TestWriterUtils.safeVariableName(id.trim().replace(Individual.LOCAL_ID_PREFIX_ACTION,""))
        return "location_$suffix"
    }


    /**
     * Check if any action requires a chain based on location headers:
     * eg a POST followed by a GET on the created resource
     */
    private fun hasChainedLocations(individual: Individual): Boolean {
        return individual.seeAllActions().any { a ->
            a is RestCallAction && a.isLocationChained()
        }
    }


    override fun addActionLinesPerType(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String) {
        addRestCallLines(action as RestCallAction, lines, result as RestCallResult, baseUrlOfSut)
    }

    private fun addRestCallLines(
        call: RestCallAction,
        lines: Lines,
        res: RestCallResult,
        baseUrlOfSut: String
    ) {

        val responseVariableName = makeHttpCall(call, lines, res, baseUrlOfSut)

        handleLocationHeader(call, res, responseVariableName, lines)
        handleResponseAfterTheCall(call, res, responseVariableName, lines)

        handleLinkInfo(call, res, responseVariableName, lines)
    }

    private fun handleLinkInfo(call: RestCallAction, res: RestCallResult, responseVariableName: String, lines: Lines) {

        if(!call.hasFollowedBackwardLink()){
            return // nothing to do
        }

        val link = call.links.find { it.statusCode == res.getStatusCode() }
        if(link == null)   {
            assert(false) // shouldn't happen, unless bug. still, don't want to crash EM for this
            return
        }

        link.parameters.forEach {
            //TODO do all cases, when implemented
            when{
                it.isBodyField() -> {
                    addExtractBodyVariable(call, res, responseVariableName, lines,  it.bodyPointer())
                }
            }
        }
    }

    private fun getLinkName(indexOfSourceAction: Int, jsonPointer: String) =
        TestWriterUtils.safeVariableName("link_${indexOfSourceAction}_$jsonPointer")

    private fun addExtractBodyVariable(
        call: RestCallAction,
        res: RestCallResult,
        responseVariableName: String,
        lines: Lines,
        jsonPointer: String
    ) {

        val index = call.positionAmongMainActions()
        val name = getLinkName(index, jsonPointer)
        val extracted = extractValueFromJsonResponse(responseVariableName, jsonPointer)

        when {
            format.isJava() -> lines.add("String $name = ")
            format.isKotlin() -> lines.add("val $name : String? = ")
            format.isJavaScript() -> lines.add("const $name = ")
            format.isPython() -> {lines.add("$name = ")}
            // should never happen
            else -> throw IllegalStateException("Unsupported format $format")
        }

        lines.append(extracted)
        lines.appendSemicolon()
    }


    override fun handleVerbEndpoint(baseUrlOfSut: String, _call: HttpWsAction, lines: Lines) {

        val call = _call as RestCallAction
        val verb = call.verb.name.lowercase()

        if (format.isCsharp()) {
            lines.append(".${StringUtils.capitalization(verb)}Async(")
        } else {
            if (verb == "trace" && format.isJavaOrKotlin()) {
                //currently, RestAssured does not have a trace() method
                lines.add(".request(io.restassured.http.Method.TRACE, ")
            } else {
                lines.add(".$verb(")
            }
        }

        if (call.usePreviousLocationId != null) {
            if (format.isJavaScript()) {
                lines.append("${TestSuiteWriter.jsImport}.")
            }

            when {
                format.isCsharp() -> lines.append("${locationVar(call.usePreviousLocationId!!)} + $baseUrlOfSut + \"${call.resolvedPath()}\"")
                format.isPython() -> lines.append("resolve_location(${locationVar(call.usePreviousLocationId!!)}, self.$baseUrlOfSut + str(\"${call.resolvedPath()}\"))")
                else -> lines.append("resolveLocation(${locationVar(call.usePreviousLocationId!!)}, $baseUrlOfSut + \"${call.resolvedPath()}\")")
            }

        } else {

            //map from variable name to link parameter object definition
            val replacements = if (call.backwardLinkReference?.isInUse() == true) {
                val ref = call.getReferenceLinkInfo()
                val dynamic = ref.first.parameters.filter { it.isBodyField() }
                val index = ref.second.positionAmongMainActions()
                dynamic.associateBy { getLinkName(index, it.bodyPointer()) }
            } else {
                mapOf()
            }

            when {
                format.isKotlin() -> lines.append("\"\${$baseUrlOfSut}")
                format.isPython() -> lines.append("self.$baseUrlOfSut + \"")
                else -> lines.append("$baseUrlOfSut + \"")
            }
            //here, we are inside an open " string

            if(replacements.isEmpty()) {
                val path = call.path.resolveOnlyPath(call.parameters)
                lines.append(escapePathElement(path))
            } else {
                val tokens = call.path.dynamicResolutionOnlyPathData(call.parameters, replacements)
                val path = tokens.joinToString(separator = "" +
                        "") {
                    if(it.second) {
                        if(format.isKotlin()) {
                            "\${${it.first}}"
                        } else {
                            "\" + ${it.first} + \""
                        }
                    } else {
                        escapePathElement(it.first)
                    }
                }
                lines.append(path)
            }

            val elements = call.path.resolveOnlyQuery(call.parameters)

            if (elements.size == 1) {
                lines.append("?${handleQuery(elements[0], replacements)}")
            } else if (elements.size > 1) {
                //several query parameters. lets have them one per line
                lines.append("?\" + ")
                lines.indented {
                    (0 until elements.lastIndex).forEach {
                        lines.add("\"${handleQuery(elements[it], replacements)}&\" + ")
                    }
                    lines.add("\"${handleQuery(elements.last(), replacements)}")
                }
            }

            lines.append("\"")

        }

        if (format.isPython()) {
            handlePythonVerbEndpoint(call, lines) { action: HttpWsAction ->
                val bodyParam = action.parameters.find { param -> param is BodyParam } as BodyParam?
                if (bodyParam != null) {
                    lines.append(", data=body")
                }
                if(config.testTimeout > 0) {
                    /*
                        As timeout at test level does not work reliably in Python, we do timeout as well in each HTTP call.
                    */
                    lines.append(", timeout=${config.testTimeout}")
                }
            }
        }

//        if (format.isCsharp()) {
//            if (isVerbWithPossibleBodyPayload(verb)) {
//                lines.append(", ")
//                handleBody(call, lines)
//            }
//            lines.append(");")
//        } else {

        lines.append(")")
//        }
    }

    private fun handleQuery(queryPair: String, replacements: Map<String, RestLinkParameter>) : String{

        val name = queryPair.substringBefore("=")
        val rep = replacements.entries.find { it.value.name == name && (it.value.scope == null || it.value.scope == RestLinkParameter.Scope.QUERY) }
        if(rep == null){
            return escapeQueryElement(queryPair)
        }

        return escapeQueryElement(name) + "=\" + " + rep.key +" + \""
    }

    private fun escapePathElement(x: String) : String{
        return GeneUtils.applyEscapes(x, mode = GeneUtils.EscapeMode.URI, format = format)
    }

    private fun escapeQueryElement(x: String) : String{
        //FIXME why the heck is this SQL mode?????????
        return GeneUtils.applyEscapes(x, mode = GeneUtils.EscapeMode.SQL, format = format)
    }

    override fun getAcceptHeader(call: HttpWsAction, res: HttpWsCallResult): String {
        return getRestAcceptHeader(call as RestCallAction, res as RestCallResult)
    }

    private fun getRestAcceptHeader(call: RestCallAction, res: RestCallResult): String {
        /*
         *  Note: using the type in result body is wrong:
         *  if you request a JSON but make an error, you might
         *  get back a text/plain with an explanation
         *
         *  TODO: get the type from the REST call
         */

        val accept = openAcceptHeader()

        var result: String

        if (call.produces.isEmpty() || res.getBodyType() == null) {
            result = "$accept\"*/*\""
        } else {

            val accepted = call.produces.filter { res.getBodyType().toString().contains(it, true) }

            result = if (accepted.size == 1) {
                "$accept\"${accepted.first()}\""
            } else {
                //FIXME: there seems to have been something or a problem
                "$accept\"*/*\""
            }
        }

        result = closeAcceptHeader(result)

        return result
    }


    private fun handleLocationHeader(call: RestCallAction, res: RestCallResult, resVarName: String, lines: Lines) {
        if (call.saveCreatedResourceLocation && !res.stopping) {

            if (!res.getHeuristicsForChainedLocation()) {

                //using what present in the "location" HTTP header

                val location = locationVar(call.creationLocationId())

                /*
                    If there is a "location" header, then it must be either empty or a valid URI.
                    If that is not the case, it would be a bug.
                    But we do not really handle it as "found fault" during the search.
                    Plus the test should not fail, although clearly a bug.
                    But in any case, if invalid URL, following HTTP calls would fail anyway

                    FIXME: should handle it as an extra oracle during the search
                 */

                when {
                    format.isJavaOrKotlin() -> {
                        val extract = "$resVarName.extract().header(\"location\")"
                        if(format.isJava()){
                            lines.add("String ")
                        } else {
                            lines.add("val ")
                        }
                        lines.append("$location = $extract")
                        lines.appendSemicolon()
                        lines.add("assertTrue(isValidURIorEmpty($location));")
                    }
                    format.isJavaScript() -> {
                        lines.add("const $location = $resVarName.header['location'];")
                        val validCheck = "${TestSuiteWriter.jsImport}.isValidURIorEmpty($location)"
                        lines.add("expect($validCheck).toBe(true);")
                    }
                    format.isCsharp() -> {
                        lines.add("Assert.True(Uri.IsWellFormedUriString($location, UriKind.Absolute) || string.IsNullOrEmpty($location));")
                    }
                    format.isPython() -> {
                        lines.add("$location = $resVarName.headers['location']")
                        val validCheck = "is_valid_uri_or_empty($location)"
                        lines.add("assert $validCheck")
                    }
                }
            } else {

                //trying to infer linked ids from HTTP response

                val baseUri: String = if (call.usePreviousLocationId != null) {
                    /* A variable should NOT be enclosed by quotes */
                    locationVar(call.usePreviousLocationId!!)
                } else {
                    /* Literals should be enclosed by quotes */
                    val path = callGraphService.resolveLocationForParentOfChildOperationUsingCreatedResource(call)
                    "\"$path\""
                }

                val idPointer = res.getResourceId()?.pointer ?: "/id"

                val extract = extractValueFromJsonResponse(resVarName, idPointer)

                when{
                    format.isJavaScript() -> lines.add("const ")
                    format.isJava() -> lines.add("String ")
                    format.isKotlin() -> lines.add("val ")
                    format.isPython()  -> lines.add("")/* nothing to do in Python */
                }
                lines.append("${locationVar(call.creationLocationId())} = $baseUri + \"/\" + $extract")
                lines.appendSemicolon()
            }
        }
    }

    override fun addTestCommentBlock(lines: Lines, test: TestCase) {

        val ind = test.test
        val ea = ind.evaluatedMainActions()

        //calls
        if(ea.isNotEmpty()){
            lines.addBlockCommentLine("Calls:")
            for(i in ea.indices){
                val x = ea[i]
                val status = (x.result as RestCallResult).getStatusCode()
                val id = x.action.getName()
                val prefix = if(ea.size == 1) "" else "${i+1} - "
                lines.addBlockCommentLine("$prefix($status) $id")
            }
        }

        //TODO move up when adding test comments to other problem types as well
        //faults
        val faults = ea.map { it.result }
            .filterIsInstance<EnterpriseActionResult>()
            .flatMap { it.getFaults() }
        if(faults.isNotEmpty()){
            if(faults.size == 1){
                lines.addBlockCommentLine("Found 1 potential fault of type-code ${faults.first().category.code}")
            } else {
                val codes = faults.asSequence().map { it.category.code }.toSet().toList().sorted()
                val codeInfo = if (codes.size == 1) {
                    " of type-code ${codes[0]}"
                } else {
                    ". Type-codes: ${codes.joinToString(", ")}"
                }
                lines.addBlockCommentLine("Found ${faults.size} potential faults$codeInfo")
            }
        }

        //examples
        val examples = getAllUsedExamples(ind.individual as RestIndividual)
            .toSet().sorted()
        if(examples.isNotEmpty()){
            if(examples.size == 1){
                lines.addBlockCommentLine("Using 1 example:")
            } else {
                lines.addBlockCommentLine("Using ${examples.size} examples:")
            }
            examples.forEach {
                lines.addBlockCommentLine("  $it")
            }

            /* Andrea: changed based on VW's feedback
            val el = StringUtils.linesWithMaxLength(examples, ", ", config.maxLengthForCommentLine)
            val opening = "Using ${examples.size} examples:"
            if(el.size == 1){
                lines.addBlockCommentLine("$opening ${el[0]}")
            } else {
                lines.addBlockCommentLine(opening)
                lines.indented {
                    el.forEach{lines.addBlockCommentLine(it)}
                }
            }
             */
        }

        //links
        val links = ea.mapNotNull { (it.action as RestCallAction).backwardLinkReference }
            .filter { it.isInUse() }
            .map { it.sourceLinkId }
        if(links.isNotEmpty()){
            if(links.size == 1){
                lines.addBlockCommentLine("Followed 1 link:")
            } else {
                lines.addBlockCommentLine("Followed ${links.size} links:")
            }
            links.forEach {
                lines.addBlockCommentLine("  $it")
            }

            /*
            val ll = StringUtils.linesWithMaxLength(links, ", ", config.maxLengthForCommentLine)
            val opening = "Followed ${links.size} links:"
            if(ll.size == 1){
                lines.addBlockCommentLine("$opening ${ll[0]}")
            } else {
                lines.addBlockCommentLine(opening)
                lines.indented {
                    ll.forEach{lines.addBlockCommentLine(it)}
                }
            }
             */
        }
    }


    private fun getAllUsedExamples(ind: RestIndividual) : List<String>{
        return ind.seeFullTreeGenes()
            .filter { it.name == RestActionBuilderV3.EXAMPLES_NAME }
            .filter { it.staticCheckIfImpactPhenotype() }
            .map { it.getValueAsRawString() }
    }
}
