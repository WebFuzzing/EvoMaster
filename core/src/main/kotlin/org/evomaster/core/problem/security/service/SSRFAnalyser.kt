package org.evomaster.core.problem.security.service

import com.google.inject.Inject
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.languagemodel.service.LanguageModelConnector
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.security.data.ActionFaultMapping
import org.evomaster.core.problem.security.data.InputFaultMapping
import org.evomaster.core.problem.security.SSRFUtil
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

class SSRFAnalyser {

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var fitness: FitnessFunction<RestIndividual>

    /**
     * Archive including test cases
     */
    @Inject
    private lateinit var archive: Archive<RestIndividual>

    @Inject
    private lateinit var languageModelConnector: LanguageModelConnector

    /**
     * [HttpCallbackVerifier] to verify HTTP callbacks for vulnerability classes
     * related to HTTP calls.
     * i.e., SSRF, XXE
     */
    @Inject
    private lateinit var httpCallbackVerifier: HttpCallbackVerifier

    /**
     * Key holds the name of the [Action], and value holds the [ActionFaultMapping].
     */
    private val actionVulnerabilityMapping: MutableMap<String, ActionFaultMapping> = mutableMapOf()

    /**
     * Individuals in the solution.
     * Derived from archive.
     */
    private lateinit var individualsInSolution: List<EvaluatedIndividual<RestIndividual>>

    /**
     * Regex pattern to match if the given string has these words anywhere on the string.
     */
    private val urlRegexPattern: Regex = "\\w*(url|source|remote|target|href|uri|link|endpoint|api|path|host)\\w*"
        .toRegex(RegexOption.IGNORE_CASE)

    /**
     * Possible URL variable names.
     * TODO: Can load from a file.
     */
    private val potentialUrlParamNames: List<String> = listOf("referer", "image")

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SSRFAnalyser::class.java)
    }

    @PostConstruct
    fun init() {
        if (config.ssrf) {
            log.debug("Initializing {}", SSRFAnalyser::class.simpleName)
        }
    }

//    FIXME: PreDestroy case out of memory problems in RestIndividualResourceTest
//    @PreDestroy
//    private fun preDestroy() {
//        if (config.ssrf) {
//            actionVulnerabilityMapping.clear()
//        }
//    }

    fun apply(): Solution<RestIndividual> {
        LoggingUtil.getInfoLogger().info("Applying {}", SSRFAnalyser::class.simpleName)

        val individualsWith2XX = getIndividualsWithStatus2XX()

        // Note: In some cases with black-box, we may not be able to get HTTP 200
        //  while, there is a possibility for a SSRF. As a temporary fix, we are
        //  selecting individuals with HTTP 400 and 422 status codes.
        val individualsWith4XX = getIndividualsWithStatus4XX()

        individualsInSolution =  individualsWith2XX + individualsWith4XX

        if (individualsInSolution.isEmpty()) {
            return archive.extractSolution()
        }

        log.debug("Total individuals before vulnerability analysis: {}", individualsInSolution.size)
        // The below steps are generic, for future extensions can be
        // accommodated easily under these common steps.

        if (httpCallbackVerifier.isActive) {
            // Reset before execution
            httpCallbackVerifier.reset()
        } else {
            httpCallbackVerifier.prepare()
        }

        // Classify endpoints with potential vulnerability classes
        classify()

        // evaluate
        evaluate()

        return archive.extractSolution()
    }

    fun anyCallsMadeToHTTPVerifier(
        action: RestCallAction,
    ): Boolean {
        /*
            WRONG: need to check that test call is using a URL, and that this trigger the fault.
            otherwise, any test with this action type would be marked as faulty

            should check the content of rcr result
         */
        val hasCallBackURL = GeneUtils
            .getAllStringFields(action.parameters)
            .any { gene ->
                httpCallbackVerifier.isCallbackURL(gene.getValueAsRawString())
            }

        if (hasCallBackURL) {
            return httpCallbackVerifier.verify(action.getName())
        }

        return false
    }

    /**
     * Classify endpoints to apply security tests based on the
     * potential security classes scope
     */
    fun classify() {
        individualsInSolution.forEach { evaluatedIndividual ->
            evaluatedIndividual.evaluatedMainActions().forEach { a ->
                val action = a.action
                if (action is RestCallAction) {
                    val actionFaultMapping = ActionFaultMapping(action.getName())
                    val inputFaultMapping: MutableMap<String, InputFaultMapping> =
                        extractRequestParameters(action.parameters)

                    inputFaultMapping.forEach { (paramName, paramMapping) ->
                        val answer = when (config.vulnerableInputClassificationStrategy) {
                            EMConfig.VulnerableInputClassificationStrategy.MANUAL -> {
                                manualClassifier(paramName, paramMapping.description)
                            }

                            EMConfig.VulnerableInputClassificationStrategy.LLM -> {
                                llmClassifier(paramName, paramMapping.description)
                            }
                        }

                        if (answer) {
                            paramMapping.addSecurityFaultCategory(DefinedFaultCategory.SSRF)
                            actionFaultMapping.addSecurityFaultCategory(DefinedFaultCategory.SSRF)
                            actionFaultMapping.isVulnerable = true
                        }
                    }

                    // Assign the param mapping
                    actionFaultMapping.params = inputFaultMapping
                    actionVulnerabilityMapping[action.getName()] = actionFaultMapping
                }
            }
        }
    }

    fun getVulnerableParameterName(action: RestCallAction): String? {
        if (actionVulnerabilityMapping.containsKey(action.getName())) {
            val mapping = actionVulnerabilityMapping[action.getName()]
            if (mapping != null) {
                return mapping.getVulnerableParameterName()
            }
        }

        return null
    }

    /**
     * A private method to identify parameter is a potentially holds URL value
     * using a Regex based approach.
     */
    private fun manualClassifier(name: String, description: String? = null): Boolean {
        if (potentialUrlParamNames.contains(name.lowercase())) {
            return true
        }
        if (urlRegexPattern.containsMatchIn(name)) {
            return true
        }
        if (description != null) {
            if (urlRegexPattern.containsMatchIn(description)) {
                return true
            }
        }
        return false
    }

    /**
     * Private method to identify parameter is a potentially holds URL value,
     * using a large language model.
     */
    private fun llmClassifier(name: String, description: String? = null): Boolean {
        val answer = if (!description.isNullOrBlank()) {
            languageModelConnector.query(
                SSRFUtil.getPromptWithNameAndDescription(
                    name,
                    description
                )
            )
        } else {
            languageModelConnector.query(
                SSRFUtil.getPromptWithNameOnly(
                    name
                )
            )
        }

        return answer != null && answer.answer == SSRFUtil.SSRF_PROMPT_ANSWER_FOR_POSSIBILITY
    }

    /**
     * Extract descriptions from the Gene of body payloads.
     */
    private fun extractRequestParameters(
        parameters: List<Param>
    ): MutableMap<String, InputFaultMapping> {
        val output = mutableMapOf<String, InputFaultMapping>()

        val genes = GeneUtils.getAllStringFields(parameters)

        genes.forEach { gene ->
            output[gene.name] = InputFaultMapping(
                gene.name,
                gene.description,
            )
        }

        return output
    }

    /**
     * Run the determined vulnerability class (from the classification) analysers.
     */
    private fun evaluate() {
        if (config.problemType == EMConfig.ProblemType.REST) {

            individualsInSolution.forEach { evaluatedIndividual ->
                evaluatedIndividual.evaluatedMainActions().forEach { a ->
                    val action = a.action
                    if (action is RestCallAction) {
                        if (actionVulnerabilityMapping.containsKey(action.getName())
                            && actionVulnerabilityMapping.getValue(action.getName()).isVulnerable
                            && evaluatedIndividual.individual is RestIndividual
                        ) {
                            val mapping = actionVulnerabilityMapping[action.getName()]

                            if (mapping != null) {
                                handleVulnerableAction(evaluatedIndividual, action)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleVulnerableAction(
        evaluatedIndividual: EvaluatedIndividual<RestIndividual>,
        action: RestCallAction
    ) {
        val copy = evaluatedIndividual.individual.copy() as RestIndividual
        // TODO: Need individual callback URL for each param?
        val callbackURL = httpCallbackVerifier.generateCallbackLink(
            action.getName()
        )

        copy.seeMainExecutableActions().forEach { action ->
            val genes = GeneUtils.getAllStringFields(action.parameters)
            genes.forEach { gene ->
                updateGeneWithCallbackURL(action.getName(), gene, callbackURL)
            }
        }

        val executedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(copy)

        if (executedIndividual != null) {
            handleExecutedIndividual(action, executedIndividual)
        }
    }

    private fun handleExecutedIndividual(
        action: RestCallAction,
        executedIndividual: EvaluatedIndividual<RestIndividual>
    ) {
        val result = httpCallbackVerifier.verify(action.getName())
        if (result) {
            val actionMapping = actionVulnerabilityMapping.getValue(action.getName())
            actionMapping.addSecurityFaultCategory(DefinedFaultCategory.SSRF)
            // Create a testing target
            archive.addIfNeeded(executedIndividual)
        }
    }

    private fun updateGeneWithCallbackURL(actionName: String, gene: Gene, callBackUrl: String) {
        if (actionVulnerabilityMapping.containsKey(actionName)) {
            val g = actionVulnerabilityMapping[actionName]!!.params[gene.name]
            if (g != null) {
                if (g.securityFaults.contains(DefinedFaultCategory.SSRF)) {
                    // Only change the param marked for SSRF
                    // This updates the children also recursively
                    gene.setFromStringValue(callBackUrl)
                }
            }
        }
    }

    private fun getIndividualsWithStatus2XX(): List<EvaluatedIndividual<RestIndividual>> {
        return RestIndividualSelectorUtils.findIndividuals(
            this.archive.extractSolution().individuals,
            statusGroup = StatusGroup.G_2xx
        )
    }
    private fun getIndividualsWithStatus4XX(): List<EvaluatedIndividual<RestIndividual>> {
        return RestIndividualSelectorUtils.findIndividuals(
            this.archive.extractSolution().individuals,
            statusCodes = listOf(422)
        )
    }
}
