package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.externalservice.rpc.DbAsExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.RPCExternalServiceAction
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.service.RPCEndpointsHandler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.max

/**
 * created by manzhang on 2021/11/26
 */
class RPCTestCaseWriter : ApiTestCaseWriter() {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(RPCTestCaseWriter::class.java)

        /**
         * name of method for customizing handling of external services for RPC problem
         */
        const val CUSTOMIZED_EXTERNAL_SERVICES = "mockRPCExternalServicesWithCustomizedHandling"

        /**
         * name of method for customizing handling of mocking database
         * note that it is only for RPC problem now
         */
        const val CUSTOMIZED_MOCK_DATABASE_OBJECTS = "mockDatabasesWithCustomizedHandling"
    }

    @Inject
    protected lateinit var rpcHandler: RPCEndpointsHandler

    override fun handleActionCalls(
            lines: Lines,
            baseUrlOfSut: String,
            ind: EvaluatedIndividual<*>,
            insertionVars: MutableList<Pair<String, String>>,
            testCaseName: String,
            testSuitePath: Path?
    ) {

        if (ind.individual is RPCIndividual){
            ind.evaluatedMainActions().forEachIndexed {index, evaluatedAction->

                lines.addEmpty()

                val call = evaluatedAction.action as RPCCallAction
                val res = evaluatedAction.result as RPCCallResult

                if (res.failedCall()) {
                    addActionInTryCatch(call,index, testCaseName, lines, res, testSuitePath, baseUrlOfSut)
                } else {
                    addActionLines(call,index, testCaseName, lines, res, testSuitePath, baseUrlOfSut)
                }
            }
        }
    }

    override fun addActionLines(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String) {

        val rpcCallAction = (action as? RPCCallAction)?: throw IllegalStateException("action must be RPCCallAction, but it is ${action::class.java.simpleName}")
        val rpcCallResult = (result as? RPCCallResult)?: throw IllegalStateException("result must be RPCCallResult, but it is ${action::class.java.simpleName}")

        // generate actions for handling external services with customized methods
        handleCustomizedExternalServiceHandling(action, index, testCaseName, true, lines, testSuitePath)
        handleCustomizedMockDatabaseHandling(action, index, testCaseName, true, lines, testSuitePath)

        val resVarName = createUniqueResponseVariableName()

        lines.addEmpty()

        // null varName representing that the test script generation fails, then skip its assertions
        val varName = handleActionExecution(lines, resVarName, rpcCallResult, rpcCallAction)
        // append additional info after the execution, eg, last statement
        appendAdditionalInfo(lines, rpcCallResult)

        if (config.enableBasicAssertions){
            if (rpcCallAction.response!=null && varName != null){
                if (rpcCallResult.hasResponse())
                    handleAssertions(lines, varName, rpcCallResult)
                else if(!rpcCallResult.isExceptionThrown()){
                    handleAssertNull(lines, varName)
                }
            }
        }

        lines.addEmpty()

        // reset mock object if any
        handleCustomizedExternalServiceHandling(action, index, testCaseName, false, lines, testSuitePath)
        handleCustomizedMockDatabaseHandling(action, index, testCaseName, false, lines, testSuitePath)
    }

    private fun handleAssertNull(lines: Lines, resVarName: String){
        if (format.isJava()){
            lines.add("assertNull($resVarName)")
            lines.appendSemicolon(config.outputFormat)
        }

    }

    private fun handleAssertions(lines: Lines, resVarName: String, rpcCallResult: RPCCallResult){
//        val responseBody = rpcCallResult.getResponseJsonValue()

        if (config.enableRPCAssertionWithInstance){
            if (rpcCallResult.getAssertionScript() != null)
                rpcCallResult.getAssertionScript()!!.split(System.lineSeparator()).forEach {
                    lines.add(it)
                }
        }

    }

    private fun handleActionExecution(lines: Lines, resVarName: String, rpcCallResult: RPCCallResult, rpcCallAction: RPCCallAction): String?{

        if (config.enablePureRPCTestGeneration){
            val script = rpcCallResult.getTestScript()
            if (script != null){
                script.split(System.lineSeparator()).forEach {
                    lines.add(it)
                }
                return rpcCallResult.getResponseVariableName()?: throw IllegalStateException("missing variable name of response")
            }else{
                log.warn("fail to get test script from em driver")
                executeActionWithSutHandler(lines, resVarName, rpcCallAction)
                return null
            }
        }else{
            val authAction = rpcHandler.getRPCAuthActionDto(rpcCallAction)
            if (authAction!=null){
                // check if it is local
                if (authAction.clientInfo == null){
                    val authInfo = "\"" + GeneUtils.applyEscapes(authAction.requestParams[0].stringValue, GeneUtils.EscapeMode.JSON, format) +"\""

                    if (format.isJavaOrKotlin()){
                        lines.add(TestSuiteWriter.controller+"."+authAction.actionName+"("+authInfo+")")

                        lines.appendSemicolon(format)
                    }

                }else
                    executeActionWithSutHandler(lines, resVarName+"_auth_"+rpcCallAction.auth.authIndex, rpcHandler.getRPCActionDtoJson(authAction))
            }
            executeActionWithSutHandler(lines, resVarName, rpcCallAction)
        }
        return resVarName
    }

    private fun appendAdditionalInfo(lines: Lines, result: RPCCallResult){
        // here, we report internal error and unexpected exception as potential faults
        if (config.outputFormat.isJavaOrKotlin() && result.hasPotentialFault()){
            lines.append("// ${result.getLastStatementForPotentialBug()}")
            if (result.isExceptionThrown())
                lines.append(" ${result.getExceptionInfo()}")
        }

    }

    override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean {
        //TODO Man: need a further check
        return false
    }

    private fun executeActionWithSutHandler(lines: Lines, resVarName: String, rpcCallAction: RPCCallAction){
        val executionJson = rpcHandler.getRPCActionJson(rpcCallAction)
        executeActionWithSutHandler(lines, resVarName, executionJson)

    }
    private fun executeActionWithSutHandler(lines: Lines, resVarName: String, executionJson: String){

        when {
            format.isKotlin() -> lines.add("val $resVarName = ${TestSuiteWriter.controller}.executeRPCEndpoint(")
            format.isJava() -> lines.add("Object $resVarName = ${TestSuiteWriter.controller}.executeRPCEndpoint(")
        }

        printExecutionJson(executionJson, lines)

        when {
            format.isKotlin() -> lines.append(")")
            format.isJava() -> lines.append(");")
        }
    }


    private fun saveJsonAndPrintReadJson(testCaseName: String, actionIndex: Int, json: String, lines: Lines, infoTag: String){
        val fileName = getFileNameToSaveMockedResponsesDtoAsJson(testCaseName, actionIndex, infoTag)

        val body = if (OutputFormatter.JSON_FORMATTER.isValid(json)) {
            OutputFormatter.JSON_FORMATTER.getFormatted(json)
        } else {
            json
        }

        val testResourcePath = Paths.get(config.testResourcePathToSaveMockedResponse)

        saveTextToDisk(body, testResourcePath, fileName)

        lines.append("${TestSuiteWriter.controller}.readFileAsStringFromTestResource(\"$fileName\")")
    }

    private fun getFileNameToSaveMockedResponsesDtoAsJson(testCaseName: String, actionIndex: Int, infoTag: String) = "${testCaseName}_${infoTag}_$actionIndex.json"


    private fun printExecutionJson(json: String, lines: Lines) {

        val body = if (OutputFormatter.JSON_FORMATTER.isValid(json)) {
            OutputFormatter.JSON_FORMATTER.getFormatted(json)
        } else {
            json
        }

        val bodyLines = body.split("\n").map { s ->
            // after applyEscapes, somehow, the format is changed, then count space here
            countFirstSpace(s) to "\" " + GeneUtils.applyEscapes(s.trim(), mode = GeneUtils.EscapeMode.BODY, format = format) + " \""
        }

        printBodyLines(lines, bodyLines)
    }

    private fun printBodyLines(lines: Lines, bodyLines: List<Pair<Int, String>>){
        lines.indented {
            bodyLines.forEachIndexed { index, line->
                lines.add(nSpace(line.first)+ line.second + (if (index != bodyLines.size - 1) "+" else ""))
            }
        }
    }

    private fun countFirstSpace(line: String) : Int{
        return max(0, line.indexOfFirst { it != ' ' })
    }

    private fun nSpace(n: Int): String{
        return (0 until n).joinToString("") { " " }
    }


    override fun addExtraInitStatement(lines: Lines) {
        if (!config.enablePureRPCTestGeneration) return

        val clientVariables = rpcHandler.getClientAndItsVariable()
        clientVariables.forEach { (t, u)->
            val getClient = "${TestSuiteWriter.controller}.getRPCClient(\"${if (format.isKotlin()) u.second.replace("$","\\$") else u.second}\")"
            when{
                config.outputFormat.isKotlin()-> lines.add("$t = $getClient as ${handleClientType(u.first)}")
                config.outputFormat.isJava() -> lines.add("$t = (${handleClientType(u.first)}) $getClient")
                else -> throw IllegalStateException("NOT SUPPORT for the format : ${config.outputFormat}")
            }
            lines.appendSemicolon(format)
        }
    }

    override fun addExtraStaticVariables(lines: Lines) {
        if (!config.enablePureRPCTestGeneration) return

        val clientVariables = rpcHandler.getClientAndItsVariable()
        clientVariables.forEach { (t, u)->
            when{
                config.outputFormat.isKotlin()-> lines.add("private lateinit var $t: ${handleClientType(u.first)}")
                config.outputFormat.isJava() -> lines.add("private static ${handleClientType(u.first)} $t")
                else -> throw IllegalStateException("NOT SUPPORT for the format : ${config.outputFormat}")
            }
            lines.appendSemicolon(format)
        }
    }

    /**
     * handle generation of customized external service handling
     * @param action is the call to be generated
     * @param index the index of action
     * @param testCaseName the test which contains the action [action]
     * @param enable a configuration to enable/disable specified mocking configuration
     * @param lines are generated lines which save the generated test scripts
     */
    private fun handleCustomizedExternalServiceHandling(action: Action, index: Int, testCaseName: String, enable: Boolean, lines: Lines, testSuitePath: Path?){
        if(config.enableCustomizedMethodForMockObjectHandling && action.parent is EnterpriseActionGroup<*>){
            val group = action.parent as EnterpriseActionGroup<*>

            /*
                now only support customized handling of external service for RPC problem
                TODO for other problems when needed
             */
            val exActions = group.getExternalServiceActions()
                    .filterIsInstance<RPCExternalServiceAction>()
                    .map { rpcHandler.transformMockRPCExternalServiceDto(it) }

            if (exActions.isNotEmpty()){
                when {
                    format.isKotlin() -> lines.add("${TestSuiteWriter.controller}.$CUSTOMIZED_EXTERNAL_SERVICES(")
                    format.isJava() -> lines.add("${TestSuiteWriter.controller}.$CUSTOMIZED_EXTERNAL_SERVICES(")
                }

                printJsonForMockObject(exActions, testCaseName, index, lines, enable, infoTag = "MockExternalServiceObjectInfo")
            }

        }
    }

    /**
     * handle generation of customized external service handling
     * @param action is the call to be generated
     * @param index the index of action
     * @param testCaseName the test which contains the action [action]
     * @param enable a configuration to enable/disable specified mocking configuration
     * @param lines are generated lines which save the generated test scripts
     */
    private fun handleCustomizedMockDatabaseHandling(action: Action, index: Int, testCaseName: String, enable: Boolean, lines: Lines, testSuitePath: Path?){
        if(config.enableCustomizedMethodForMockObjectHandling && action.parent is EnterpriseActionGroup<*>){
            val group = action.parent as EnterpriseActionGroup<*>

            val mockDbActions = group.getExternalServiceActions()
                .filterIsInstance<DbAsExternalServiceAction>()
                .map { rpcHandler.transformMockDatabaseDto(it) }

            if (mockDbActions.isNotEmpty()){
                when {
                    format.isKotlin() -> lines.add("${TestSuiteWriter.controller}.$CUSTOMIZED_MOCK_DATABASE_OBJECTS(")
                    format.isJava() -> lines.add("${TestSuiteWriter.controller}.$CUSTOMIZED_MOCK_DATABASE_OBJECTS(")
                }

                printJsonForMockObject(mockDbActions, testCaseName, index, lines, enable, infoTag = "MockDatabaseObjectInfo")
            }

        }
    }

    private fun printJsonForMockObject(mockObj : Any, testCaseName: String, index: Int, lines: Lines, enable: Boolean, infoTag: String){
        val mockedConfigAsJson = rpcHandler.getJsonStringFromDto(mockObj)

        if (config.saveMockedResponseAsSeparatedFile){
            if (config.testResourcePathToSaveMockedResponse.isBlank())
                throw IllegalArgumentException("testResourcePathToSaveMockedResponse cannot be empty if it is required to save mocked responses in separated files")
            saveJsonAndPrintReadJson(testCaseName,index,mockedConfigAsJson, lines, infoTag)
        }else
            printExecutionJson(mockedConfigAsJson, lines)

        when {
            format.isKotlin() -> lines.append(",$enable)")
            format.isJava() -> lines.append(",$enable);")
        }
    }

    fun resetExternalServicesWithCustomizedMethod() : String {
        if (!format.isJavaOrKotlin())
            throw IllegalStateException("Only support to generate Java/Kotlin tests with a reset of RPC external services with customized method")
        return "${TestSuiteWriter.controller}.$CUSTOMIZED_EXTERNAL_SERVICES(null,false)${if (format.isJava()) ";" else ""}"
    }

    fun resetMockDatabaseObjectWithCustomizedMethod() : String {
        if (!format.isJavaOrKotlin())
            throw IllegalStateException("Only support to generate Java/Kotlin tests with a reset of RPC external services with customized method")
        return "${TestSuiteWriter.controller}.$CUSTOMIZED_MOCK_DATABASE_OBJECTS(null,false)${if (format.isJava()) ";" else ""}"
    }

    /*
        the inner class in java could be represented with $ in string format
        for instance org.thrift.ncs.client.NcsService$Client,
        then we need to further handle it
     */
    private fun handleClientType(clientType: String) = clientType.replace("\$",".")

    override fun additionalTestHandling(tests: List<TestCase>) {
        if (!config.enableRPCCustomizedTestOutput) return

        try {
            rpcHandler.handleCustomizedTests(tests.map { t-> t.test as EvaluatedIndividual<RPCIndividual> })
        }catch (e : Exception){
            log.warn("Fail to handle customized tests: ${e.message}")
        }
    }

}
