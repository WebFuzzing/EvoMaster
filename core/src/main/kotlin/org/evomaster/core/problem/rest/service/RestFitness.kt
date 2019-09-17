package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.IdMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.gene.regex.RegexGene

open class RestFitness : AbstractRestFitness<RestIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestFitness::class.java)
    }

    @Inject(optional = true)
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var sampler: RestSampler

    override fun doCalculateCoverage(individual: RestIndividual): EvaluatedIndividual<RestIndividual>? {

        rc.resetSUT()

        doInitializingActions(individual)

        individual.enforceCoherence()

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        for (i in 0 until individual.seeActions().size) {

            val a = individual.seeActions()[i]

            registerNewAction(a, i)

            var ok = false

            if (a is RestCallAction) {
                ok = handleRestCall(a, actionResults, chainState)
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }
        }

        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list
        val ids = randomness.choose(
                archive.notCoveredTargets().filter { !IdMapper.isLocal(it) },
                100).toSet()

        val dto = rc.getTestResults(ids)
        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {

                if (!config.useMethodReplacement &&
                        t.descriptiveId.startsWith(ObjectiveNaming.METHOD_REPLACEMENT)) {
                    return@forEach
                }

                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        handleExtra(dto, fv)

        handleResponseTargets(fv, individual.seeActions(), actionResults, dto.additionalInfoList)

        if (config.expandRestIndividuals) {
            expandIndividual(individual, dto.additionalInfoList)
        }

        if (config.baseTaintAnalysisProbability > 0) {
            doTaintAnalysis(individual, dto.additionalInfoList)
        }

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults)
    }

    private fun registerNewAction(action: RestAction, index: Int){

        rc.registerNewAction(ActionDto().apply {
            this.index = index
            //for now, we only include specialized regex
            this.inputVariables = action.seeGenes()
                    .flatMap { it.flatView() }
                    .filterIsInstance<StringGene>()
                    .filter { it.getSpecializationGene() != null && it.getSpecializationGene() is RegexGene}
                    .map { it.getSpecializationGene()!!.getValueAsRawString()}
        })
    }

    private fun doTaintAnalysis(individual: RestIndividual, additionalInfoList: List<AdditionalInfoDto>) {

        /*
            Analyze if any tainted value was used in the SUT in some special way.
            If that happened, then such info would end up in the AdditionalInfoDto.
            Then, we would extend the genotype (but not the phenotype!!!) of this test.
         */

        Lazy.assert { individual.seeActions().size == additionalInfoList.size }

        for (i in 0 until additionalInfoList.size) {

            val dto = additionalInfoList[i]
            if (dto.stringSpecializations == null || dto.stringSpecializations.isEmpty()) {
                continue
            }

            val action = individual.seeActions()[i]

            for (entry in dto.stringSpecializations.entries) {

                if (entry.value.isEmpty()) {
                    throw IllegalArgumentException("No specialization info for value ${entry.key}")
                }

                val specs = entry.value.map {
                    StringSpecializationInfo(
                            StringSpecialization.valueOf(it.stringSpecialization),
                            it.value)
                }

                val stringGene = action.seeGenes()
                        .flatMap { it.flatView() }
                        .filterIsInstance<StringGene>()
                        .find { it.getValueAsRawString() == entry.key }

                if (stringGene == null) {
                    /*
                        This can happen if the taint input is manipulated, but still with
                        same prefix and postfix
                     */
                    log.debug("No taint input '${entry.key}' in action nr. $i")
                } else {
                    stringGene.addSpecializations(specs, randomness)
                }
            }
        }

    }

    override fun doInitializingActions(ind: RestIndividual) {

        if (ind.dbInitialization.none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            return
        }

        val dto = DbActionTransformer.transform(ind.dbInitialization)

        val ok = rc.executeDatabaseCommand(dto)
        if (!ok) {
            //this can happen if we do not handle all constraints
            LoggingUtil.uniqueWarn(log, "Failed in executing database command")
        }
    }

    override fun hasParameterChild(a: RestCallAction): Boolean {
        return sampler.seeAvailableActions()
                .filterIsInstance<RestCallAction>()
                .map { it.path }
                .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }
}