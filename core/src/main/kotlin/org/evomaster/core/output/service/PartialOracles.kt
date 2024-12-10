package org.evomaster.core.output.service


/**
 * [PartialOracles] are meant to be a way to handle different types of soft assertions/expectations (name may change in future)
 *
 * The idea is that any new type of expectation is a partial oracle (again, name may change, if anything more appropriate
 * emerges). For example, if a return object is specified in the Swagger for a given endpoint with a given status code,
 * then the object returned should have the same structure as the Swagger reference (see [responseStructure] below.
 *
 * They are "partial" because failing such a test is not necessarily indicative of a bug, as it could be some sort
 * or shortcut or something (and since REST semantics are not strictly enforced, it cannot be an assert). Nevertheless,
 * it would be a break with expected semantics and could be indicative of a fault or design problem.
 *
 * The [PartialOracles] would (in future) each receive their own variable to turn on or off, and only those selected
 * would be added to the code. So they should be independent from each other. The idea is that, either during generation
 * or during execution, the user can decide if certain partial oracles are of interest at the moment, and turn then
 * on or off as required.
 *
 */
@Deprecated("No longer used")
class PartialOracles {

//    @Inject
//    private lateinit var config: EMConfig
//
//    private val objectGenerator = ObjectGenerator()
//
//    private val oracles = mutableListOf<ImplementedOracle>()
//    private val expectationsMasterSwitch = "ems"
//
//    fun setupForRest(schema: OpenAPI, config: EMConfig){
//
//        oracles.add(SupportedCodeOracle())
//        oracles.add(SchemaOracle())
//
//        oracles.forEach {
//            it.setObjectGenerator(objectGenerator)
//        }
//
//        objectGenerator.setSwagger(schema, config.enableSchemaConstraintHandling)
//    }
//
//    /**
//     * The [variableDeclaration] method handles the generation of auxiliary variables for the partial oracles.
//     * The parameters [lines] and [format] are received from the [TestCaseWriter].
//     * The parameter [active] records which partial oracles are active (i.e. which actually generate
//     * a failing expectation). [active] is a mutable map, where the key is a string containing the name of
//     * an [ImplementedOracle], and the key is a boolean indicating whether the oracle is active (i.e. will
//     * generate a failing expectation, and thus require the additional variables) or inactive (i.e. it will
//     * not generate a failing expectation and thus can be skipped).
//     *
//     * The goal of this method is to ensure that only relevant variables are generated (i.e. to avoid
//     * generating stub variables that are never used).
//     */
//    fun variableDeclaration(lines: Lines, format: OutputFormat, active: Map<String, Boolean>){
//        for (oracle in oracles){
//            if(active.get(oracle.getName()) == true) {
//                        oracle.variableDeclaration(lines, format)
//            }
//        }
//    }
//
//    fun addExpectations(call: RestCallAction, lines: Lines, res: HttpWsCallResult, name: String, format: OutputFormat) {
//        val generates = oracles.any {
//            it.generatesExpectation(call, res)
//        }
//        if (!generates) return
//        lines.add("expectationHandler.expect($expectationsMasterSwitch)")
//        lines.indented {
//            for (oracle in oracles) { oracle.addExpectations(call, lines, res, name, format) }
//            if (format.isJava()) { lines.append(";") }
//        }
//    }
//
//
//
//
//    fun selectForClustering(action: EvaluatedAction): Boolean{
//            return oracles.any { oracle ->
//                oracle.selectForClustering(action)
//            }
//    }
//
//    /**
//     * The [generatesExpectation] method evaluates is, for any given test case, any expectation is generated.
//     * This is used in the [ExpectationsWriter] to ensure that, if no (failing) expectation is generated,
//     * the variable [ExpectationHandler] is not added to the generated code.
//     */
//
//    fun generatesExpectation(individual: EvaluatedIndividual<RestIndividual>): Boolean{
//        return oracles.any { oracle ->
//            individual.evaluatedMainActions().any {
//                oracle.generatesExpectation(
//                        (it.action as RestCallAction),
//                        (it.result as HttpWsCallResult)
//                )
//            }
//        }
//    }
//
//    fun generatesExpectation(call: RestCallAction, res: HttpWsCallResult): Boolean{
//        return oracles.any { oracle ->
//            oracle.generatesExpectation( call, res)
//        }
//    }
//
//    /**
//     * [failByOracle] is an auxiliary method that generates a mutable map. The key for each entry in this
//     * mutable map is the name of an [ImplementedOracle], and the value is a list of [EvaluatedIndividual]
//     * that are selected for clustering (i.e. for which a failing expectation is generated) by that
//     * specific oracle.
//     *
//     * The values are sorted by size, measured in terms of number of [EvaluatedAction] in the respective
//     * [EvaluatedIndividual]. This sorting ensures that the shortest of the available Individuals is selected
//     * for the executive summary.
//     *
//     * The method is used to create the executive summary in the [TestSuiteSplitter]. The executive
//     * summary is created by selecting from each of the sets returned by the [failByOracle] method that
//     * [EvaluatedIndividual] fulfils some additional requirements (for example, does not repeat - if an
//     * [EvaluatedIndividual] has failing expectations generated by more than one oracle, it should not be
//     * repeated in the executive summary, but the next shortest candidate (if available) will be selected
//     * instead.
//     *
//     * The method uses MutableMap and MutableList only as a necessity for selection and sorting, and no
//     * changes are made to the [EvaluatedIndividual] objects themselves.
//     *
//     */
//    fun failByOracle(individuals: List<EvaluatedIndividual<ApiWsIndividual>>): MutableMap<String, MutableList<EvaluatedIndividual<ApiWsIndividual>>>{
//        val oracleInds = mutableMapOf<String, MutableList<EvaluatedIndividual<ApiWsIndividual>>>()
//        oracles.forEach { oracle ->
//            val failindInds = individuals.filter {
//                it.evaluatedMainActions().any { oracle.selectForClustering(it) }
//            }.toMutableList()
//            failindInds.sortBy { it.evaluatedMainActions().size }
//            oracleInds.put(oracle.getName(), failindInds)
//        }
//        return oracleInds
//    }
//
//    fun activeOracles(individuals: List<EvaluatedIndividual<*>>): MutableMap<String, Boolean>{
//        val active = mutableMapOf<String, Boolean>()
//        oracles.forEach { oracle ->
//            active.put(oracle.getName(), individuals.any { individual ->
//                individual.evaluatedMainActions().any {
//                    it.action is RestCallAction && oracle.generatesExpectation(
//                            (it.action as RestCallAction),
//                            (it.result as HttpWsCallResult)
//                    )
//                } })
//        }
//        return active
//    }
//
//    fun activeOracles(call: RestCallAction, res: HttpWsCallResult): MutableMap<String, Boolean>{
//        val active = mutableMapOf<String, Boolean>()
//        oracles.forEach { oracle ->
//            active.put(oracle.getName(), oracle.generatesExpectation(call, res))
//        }
//        return active
//    }
//
//    fun adjustName(): MutableList<ImplementedOracle>{
//        return oracles.filter { !it.adjustName().isNullOrBlank() }.toMutableList()
//    }

}