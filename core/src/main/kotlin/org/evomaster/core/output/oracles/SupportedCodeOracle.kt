package org.evomaster.core.output.oracles

/**
 * The [SupportedCodeOracle] class generates an expectation and writes it to the code.
 *
 * A comparison is made between the status code of the [RestCallResult] and the supported return codes as defined
 * by the schema. If the actual code is not supported by the schema, the relevant expectation is generated and added
 * to the code.
 */
@Deprecated("No longer used")
class SupportedCodeOracle : ImplementedOracle() {
//    private val variableName = "sco"
//    private lateinit var objectGenerator: ObjectGenerator
//    private val log: Logger = LoggerFactory.getLogger(SupportedCodeOracle::class.java)
//
//    override fun variableDeclaration(lines: Lines, format: OutputFormat) {
//        lines.add("/**")
//        lines.add("* $variableName - supported code oracle - checking that the response status code is among those supported according to the schema")
//        lines.add("*/")
//        when{
//            format.isJava() -> lines.add("private static boolean $variableName = false;")
//            format.isKotlin() -> lines.add("private val $variableName = false")
//            format.isJavaScript() -> lines.add("const $variableName = false;")
//        }
//
//    }
//
//    override fun addExpectations(call: RestCallAction, lines: Lines, res: HttpWsCallResult, name: String, format: OutputFormat) {
//        //if(!supportedCode(call, res)){
//        if(generatesExpectation(call, res)){
//            // The code is not among supported codes, so an expectation will be generated
//            //val actualCode = res.getStatusCode() ?: 0
//            //lines.add(".that($oracleName, Arrays.asList(${getSupportedCode(call)}).contains($actualCode))")
//            val supportedCodes = getSupportedCode(call)
//            //BMR: this will be a problem if supportedCode contains both codes and default...
//            if(supportedCodes.contains("0")){
//                lines.add("// WARNING: the code list seems to contain an unsupported code (0 is not a valid HTTP code). This could indicate a problem with the schema. The issue has been logged.")
//                supportedCodes.remove("0")
//                LoggingUtil.uniqueWarn(log, "The list of supported codes appears to contain an unsupported code (0 is not a valid HTTP code). This could indicate a problem with the schema.")
//                //TODO: if a need arises for more involved checks, refactor this
//            }
//            val supportedCode = supportedCodes.joinToString(", ")
//
//            if(supportedCode.equals("")){
//                lines.add("/*")
//                lines.add(" Note: No supported codes appear to be defined. https://swagger.io/docs/specification/describing-responses/.")
//                lines.add(" This is somewhat unexpected, so the code below is likely to lead to a failed expectation")
//                lines.add("*/")
//                when {
//                    format.isJava() -> lines.add(".that($variableName, Arrays.asList().contains($name.extract().statusCode()))")
//                    format.isKotlin() -> lines.add(".that($variableName, listOf<Int>().contains($name.extract().statusCode()))")
//                }
//            }
//            //TODO: check here if supported code contains 0 (or maybe check against a list of "acceptable" codes
//            else when {
//                format.isJava() -> lines.add(".that($variableName, Arrays.asList($supportedCode).contains($name.extract().statusCode()))")
//                format.isKotlin() -> lines.add(".that($variableName, listOf<Int>($supportedCode).contains($name.extract().statusCode()))")
//            }
//        }
//    }
//    fun supportedCode(call: RestCallAction, res: HttpWsCallResult): Boolean{
//        val code = res.getStatusCode().toString()
//        val validCodes = getSupportedCode(call)
//        return (validCodes.contains(code) || validCodes.contains("default"))
//    }
//
//    fun getSupportedCode(call: RestCallAction): MutableSet<String>{
//        val verb = call.verb
//        val path = retrievePath(objectGenerator, call)
//        val result = when (verb){
//            HttpVerb.GET -> path?.get
//            HttpVerb.POST -> path?.post
//            HttpVerb.PUT -> path?.put
//            HttpVerb.DELETE -> path?.delete
//            HttpVerb.PATCH -> path?.patch
//            HttpVerb.HEAD -> path?.head
//            HttpVerb.OPTIONS -> path?.options
//            HttpVerb.TRACE -> path?.trace
//            else -> null
//        }
//        return result?.responses?.keys ?: mutableSetOf()
//    }
//
//    override fun setObjectGenerator(gen: ObjectGenerator){
//        objectGenerator = gen
//    }
//
//    override fun generatesExpectation(call: RestCallAction, res: HttpWsCallResult): Boolean {
//        if(this::objectGenerator.isInitialized){
//             return !supportedCode(call, res)
//        }
//        return false
//    }
//
//    override fun generatesExpectation(individual: EvaluatedIndividual<*>): Boolean {
//        if(individual.individual !is RestIndividual) return false
//        if(!this::objectGenerator.isInitialized) return false
//        val gens = individual.evaluatedMainActions().any {
//            !supportedCode(it.action as RestCallAction, it.result as HttpWsCallResult)
//        }
//        return gens
//    }
//
//    override fun selectForClustering(action: EvaluatedAction): Boolean {
//        return if (action.result is HttpWsCallResult
//                && action.action is RestCallAction
//                &&this::objectGenerator.isInitialized
//                && !(action.action as RestCallAction).skipOracleChecks
//        )
//            !supportedCode(action.action as RestCallAction, action.result as HttpWsCallResult)
//        else false
//    }
//
//    override fun getName(): String {
//        return "CodeOracle"
//    }
}