package org.evomaster.core.problem.security.service

import com.google.inject.Inject
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.languagemodel.service.LanguageModelConnector
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.security.data.ActionFaultMapping
import org.evomaster.core.problem.security.data.InputFaultMapping
import org.evomaster.core.problem.security.SSRFUtil
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.optional.ChoiceGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PreDestroy

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

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SSRFAnalyser::class.java)
    }

    @PreDestroy
    private fun preDestroy() {
        if (config.ssrf) {
            actionVulnerabilityMapping.clear()
        }
    }


    fun apply(): Solution<RestIndividual> {
        LoggingUtil.Companion.getInfoLogger().info("Applying {}", SSRFAnalyser::class.simpleName)

        // extract individuals from the archive
        val individuals = this.archive.extractSolution().individuals

        individualsInSolution =
            RestIndividualSelectorUtils.findIndividuals(
                individuals,
                statusCodes = listOf(200, 201)
            )

        if (individualsInSolution.isEmpty()) {
            return archive.extractSolution()
        }

        if (!httpCallbackVerifier.isActive) {
            httpCallbackVerifier.initWireMockServer()
        } else {
            httpCallbackVerifier.resetHTTPVerifier()
        }

        log.debug("Total individuals before vulnerability analysis: {}", individuals.size)
        // The below steps are generic, for future extensions can be
        // accommodated easily under these common steps.

        // Classify endpoints with potential vulnerability classes
        classify()

        // execute
        analyse()

        // TODO: This is for development, remove it later
        val individualsAfterExecution = RestIndividualSelectorUtils.findIndividuals(
            this.archive.extractSolution().individuals,
            statusCodes = listOf(200, 201)
        )
        log.debug("Total individuals after vulnerability analysis: {}", individualsAfterExecution.size)

        return archive.extractSolution()
    }

    fun hasVulnerableInputs(
        action: RestCallAction,
    ): Boolean {
        /*
            WRONG: need to check that test call is using a URL, and that this trigger the fault.
            otherwise, any test with this action type would be marked as faulty

            should check the content of rcr result
         */

        if (!actionVulnerabilityMapping.containsKey(action.getName())) {
            return false
        }

        var hasCallbackURL = false

        action.parameters.forEach { param ->
            val genes = getStringGenesFromParam(param.seeGenes())
            // TODO: Handle param types
            genes.forEach { gene ->
                hasCallbackURL = httpCallbackVerifier.isCallbackURL(gene.getValueAsRawString())
            }
        }

        return hasCallbackURL && httpCallbackVerifier.verify(action.getName())
    }

    /**
     * Classify endpoints to apply security tests based on the
     * potential security classes scope
     */
    fun classify() {
        // TODO: We need to store word bag of potential input names
        //  if we are going to classify using the variable names.
        //  Other approach is to rely on the API doc with explicit
        //  definitions of potential vulnerability classes.
        //  This is for SSRF.
        //  For SQLi we can consider individuals with SQL actions.
        //  Are we going mark potential vulnerability classes as one time
        //  job or going to evaluate each time (which is costly).

        when (config.vulnerableInputClassificationStrategy) {
            EMConfig.VulnerableInputClassificationStrategy.MANUAL -> {
                manualClassifier()
            }

            EMConfig.VulnerableInputClassificationStrategy.LLM -> {
                llmClassifier()
            }
        }
    }

    /**
     * TODO: Classify based on manual
     * TODO: Need to rename the word manual to something meaningful later
     */
    private fun manualClassifier() {
        // TODO: Can use the extracted CSV to map the parameter name
        //  to the vulnerability class.
    }


    /**
     * Private method to classify parameters using a large language model.
     */
    private fun llmClassifier() {
        // For now, we consider only the individuals selected from [Archive]
        // TODO: This can be isolated to classify at the beginning of the search
        individualsInSolution.forEach { evaluatedIndividual ->
            evaluatedIndividual.evaluatedMainActions().forEach { a ->
                val action = a.action
                if (action is RestCallAction && !actionVulnerabilityMapping.containsKey(action.getName())) {
                    val actionFaultMapping = ActionFaultMapping(action.getName())
                    val inputFaultMapping: MutableMap<String, InputFaultMapping> =
                        extractBodyParameters(action.parameters)

                    inputFaultMapping.forEach { paramName, paramMapping ->
                        val answer = if (!paramMapping.description.isNullOrBlank()) {
                            languageModelConnector.query(
                                SSRFUtil.Companion.getPromptWithNameAndDescription(
                                    paramMapping.name,
                                    paramMapping.description
                                )
                            )
                        } else {
                            languageModelConnector.query(
                                SSRFUtil.Companion.getPromptWithNameOnly(
                                    paramMapping.name
                                )
                            )
                        }

                        if (answer != null && answer.answer == SSRFUtil.Companion.SSRF_PROMPT_ANSWER_FOR_POSSIBILITY) {
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

    /**
     * Extract descriptions from the Gene of body payloads.
     */
    private fun extractBodyParameters(
        parameters: List<Param>
    ): MutableMap<String, InputFaultMapping> {
        val output = mutableMapOf<String, InputFaultMapping>()

        parameters.forEach { param ->
            when (param) {
                is BodyParam -> {
                    val genes = getStringGenesFromParam(param.seeGenes())

                    genes.forEach { gene ->
                        output[gene.name] = InputFaultMapping(
                            gene.name,
                            gene.description,
                        )
                    }
                }

                is HeaderParam -> {
                    val genes = getStringGenesFromParam(param.seeGenes())

                    genes.forEach { gene ->
                        output[gene.name] = InputFaultMapping(
                            gene.name,
                            gene.description,
                        )
                    }
                }

                is QueryParam -> {
                    val genes = getStringGenesFromParam(param.seeGenes())

                    genes.forEach { gene ->
                        output[gene.name] = InputFaultMapping(
                            gene.name,
                            gene.description,
                        )
                    }
                }

                else -> {
                    // Do nothing
                }
            }
        }

        return output
    }

    /**
     * Run the determined vulnerability class (from the classification) analysers.
     */
    private fun analyse() {
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
            action.parameters.forEach { param ->
                updateGeneWithCallbackURL(action.getName(), param.primaryGene(), callbackURL)
            }
        }

        val executedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(copy)

        if (executedIndividual != null) {
            handleExecutedIndividual(action, executedIndividual, callbackURL)
        }
    }

    private fun handleExecutedIndividual(
        action: RestCallAction,
        executedIndividual: EvaluatedIndividual<RestIndividual>,
        callbackURL: String
    ) {
        actionVulnerabilityMapping.getValue(action.getName()).httpCallbackURL = callbackURL
        val result = httpCallbackVerifier.verify(action.getName())
        if (result) {
            val actionMapping = actionVulnerabilityMapping.getValue(action.getName())
            actionMapping.isExploitable = true
            actionMapping.addSecurityFaultCategory(DefinedFaultCategory.SSRF)
            // Create a testing target
            archive.addIfNeeded(executedIndividual)
        }
    }

    private fun updateGeneWithCallbackURL(actionName: String, primaryGene: Gene, callBackUrl: String) {
        primaryGene.getViewOfChildren().forEach { gene ->
            if (actionVulnerabilityMapping.containsKey(actionName)) {
                val g = actionVulnerabilityMapping[actionName]!!.params[gene.name]
                if (g!!.securityFaults.contains(DefinedFaultCategory.SSRF)) {
                    // Only change the param marked for SSRF
                    gene.setFromStringValue(callBackUrl)
                }
            }
        }
    }

    private fun getStringGenesFromParam(genes: List<Gene>) : List<Gene> {
        val output = mutableListOf<Gene>()

        genes.forEach { gene ->
            when (gene) {
                is StringGene -> {
                    output.add(gene)
                }
                is OptionalGene -> {
                    output.addAll(getStringGenesFromParam(gene.getViewOfChildren()))
                }
                is ObjectGene -> {
                    output.addAll(getStringGenesFromParam(gene.getViewOfChildren()))
                }
                is ChoiceGene<*> -> {
                    output.addAll(getStringGenesFromParam(gene.getViewOfChildren()))
                }
                is CustomMutationRateGene<*> -> {
                    output.addAll(getStringGenesFromParam(gene.getViewOfChildren()))
                }
            }
        }

        return output
    }
}
