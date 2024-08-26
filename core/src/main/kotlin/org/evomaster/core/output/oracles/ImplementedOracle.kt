package org.evomaster.core.output.oracles

@Deprecated("No longer used")
abstract class ImplementedOracle {

//    private val log: Logger = LoggerFactory.getLogger(ImplementedOracle::class.java)
//
//    /**
//     * [variableDeclaration] handles, for each [ImplementedOracle] the process of generating variables that
//     * activate and deactivate the checking of that particular oracle, along with a short
//     * comment explaining its purpose.
//     */
//    abstract fun variableDeclaration(lines: Lines, format: OutputFormat)
//
//    /**
//     * [addExpectations] handles the process of generating the code for failing expectations, as evaluated
//     * by the particular oracle implementation.
//     */
//    abstract fun addExpectations(call: RestCallAction, lines: Lines, res: HttpWsCallResult, name: String, format: OutputFormat)
//
//    /**
//     * The [setObjectGenerator] method is used to add the [ObjectGenerator] to individual oracles. At the time
//     * of writing, both implemented oracles require information from this object (schemas and supported codes).
//     */
//    abstract fun setObjectGenerator(gen: ObjectGenerator)
//
//    /**
//     * The [generatesExpectation] method is used to determine if, for a given [EvaluatedAction], the
//     * [ImplementedOracle] generates an expectation.
//     */
//    abstract fun generatesExpectation(call: RestCallAction, res: HttpWsCallResult): Boolean
//
//    /**
//     * The [generatesExpectation] method is used to determine if, for a given [EvaluatedIndividual],
//     * the [ImplementedOracle] generates an expectation.
//     */
//    abstract fun generatesExpectation(individual: EvaluatedIndividual<*>): Boolean
//
//    /**
//     * The [selectForClustering] method determines if a particular action is selected for clustering,
//     * according to the current [ImplementedOracle]. Normally, selection is based on whether or not
//     * an [ImplementedOracle] generates an expectation for the given [EvaluatedAction]. However, additional
//     * conditions may be imposed (for example, ensuring that the [EvaluatedAction] is of a particular type,
//     * and that is has a call and a result of types [RestCallAction] and [RestCallResult], respectively).
//     *
//     * Additional conditions may be required by future [ImplementedOracle] objects.
//     */
//    abstract fun selectForClustering(action: EvaluatedAction): Boolean
//
//    /**
//     * [getName] returns the name of the oracle. This is used for identification, both in the generated
//     * code and in determining oracle status.
//     */
//    abstract fun getName(): String
//
//    /**
//     * The [adjustName] method returns a String with a name adjustment, or null if the [ImplementedOracle]
//     * does not adjust the [TestCase] name. The name adjustment is appended to the existing name.
//     */
//    open fun adjustName(): String?{
//        return null
//    }
//
//    /**
//     * Some OpenAPI paths are called inconsistently (e.g. with or without "/api" appended as a prefix).
//     *
//     * This is a workaround (ScoutAPI only sees paths without the prefix, others with the prefix).
//     * Longer term, this could also be a place to handle any additional peculiarities with SUT specific
//     * OpenAPI standards.
//     *
//     * The same applies where the prefix is "v2" (e.g. language tools).
//     */
//
//    fun retrievePath(objectGenerator: ObjectGenerator, call: RestCallAction): PathItem? {
//        val swagger = objectGenerator.getSwagger()
//        val basePath = RestActionBuilderV3.getBasePathFromURL(swagger)
//
//        // This is likely where the problem is.
//        /*
//        If the basepath is empty - it shows up as "/" leading to "//" in the final path.
//        if the basepath is not empty, ignoring it also causes problems.
//         */
//
//        val adjustedBasePath = if (basePath.endsWith("/")) {
//            basePath.dropLast(1)
//        }
//        else{
//            basePath
//        }
//
//        val possibleItems = objectGenerator.getSwagger().paths.filter{ e ->
//            call.path.toString().contentEquals(adjustedBasePath + e.key)
//            //call.path.toString().contentEquals(basePath+e.key)
//        }
//
//        val result = when (possibleItems.size){
//            0 -> null
//            1 -> possibleItems.entries.first().value
//            else -> {
//                // This should not happen unless multiples paths match the call. But it's technically not impossible. Then pick the longest key (to avoid matching root "/", see ProxyPrint).
//                // I'd prefer not to throw exceptions that would disrupt the rest of the writing process.
//                //log.warn("There seem to be multiple paths matching a call: ${call.verb}${call.path}. Only one will be returned.")
//                val possibleItemString = possibleItems.entries.joinToString { it.key }
//                LoggingUtil.Companion.uniqueWarn(log, "There seem to be multiple paths matching a call: ${call.verb}\n${possibleItemString}. Only one will be returned.")
//                possibleItems.entries.maxByOrNull{ it.key.length }?.value
//            }
//        }
//
//        return result
//    }
}