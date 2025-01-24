package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.api.service.ApiWsSampler
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.scheduletask.ScheduleTaskAction
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * created by manzhang on 2021/11/25
 */
class RPCSampler: ApiWsSampler<RPCIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RPCSampler::class.java)
    }

    @Inject
    protected lateinit var configuration: EMConfig


    @Inject
    protected lateinit var rpcHandler: RPCEndpointsHandler


    protected val adHocInitialIndividuals: MutableList<RPCIndividual> = mutableListOf()

    /**
     * Set of available schedule task actions that can be used to define a test case
     *
     * Key -> schedule task type plus task name
     *
     * Value -> an action
     */
    protected val scheduleActionCluster: MutableMap<String, Action> = mutableMapOf()


    @PostConstruct
    fun initialize() {
        log.debug("Initializing {}", RPCSampler::class.simpleName)

        if(configuration.blackBox && !configuration.bbExperiments){
            throw IllegalStateException("Not support black-box testing for RPC with interface")
        }

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        val infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val problem = infoDto.rpcProblem
                ?: throw IllegalStateException("Missing problem definition object")

        rpcHandler.initActionCluster(problem, actionCluster,scheduleActionCluster, infoDto)

        initSqlInfo(infoDto)

        initAdHocInitialIndividuals(infoDto)

        if (config.seedTestCases)
            initSeededTests(infoDto)

        updateConfigBasedOnSutInfoDto(infoDto)
        log.debug("Done initializing {}", RPCSampler::class.simpleName)
    }

    override fun smartSample(): RPCIndividual {
        if (adHocInitialIndividuals.isNotEmpty()) {
            return adHocInitialIndividuals.removeAt(adHocInitialIndividuals.size - 1)
        }
        return sampleAtRandom()
    }



    /**
     * sample an action from [actionCluster] at random
     * @param noAuthProbability specifies a probability which does not apply any auth
     */
    fun sampleRandomAction(noAuthProbability: Double = 0.05, noSeedProbability: Double = 0.05): RPCCallAction {
        val action = randomness.choose(actionCluster).copy() as RPCCallAction
        action.doInitialize(randomness)
        if (randomness.nextBoolean(noAuthProbability)){
            action.setNoAuth()
        }else
            rpcHandler.actionWithRandomAuth(action)

        rpcHandler.actionWithRandomSeeded(action, noSeedProbability)

        return action
    }

    /**
     * sample a schedule task action from [scheduleActionCluster] at random
     * @param noSeedProbability specifies a probability which does not apply seeded one
     */
    fun sampleRandomScheduleTaskAction(noSeedProbability: Double = 0.05) : ScheduleTaskAction {
        val action = randomness.choose(scheduleActionCluster).copy() as ScheduleTaskAction
        action.doInitialize(randomness)
        rpcHandler.scheduleActionWithRandomSeeded(action, noSeedProbability)
        return action
    }

    override fun sampleAtRandom(): RPCIndividual {
        val len = randomness.nextInt(1, config.maxTestSize)
        val actions : MutableList<ActionComponent> = (0 until len).map {
            val a = sampleRandomAction(0.05)
            EnterpriseActionGroup(mutableListOf(a), RPCCallAction::class.java)
        }.toMutableList()

        val leftlen = config.maxTestSize - len
        if (leftlen > 0 && randomness.nextBoolean(config.probOfSamplingScheduleTask)){
            val slen = randomness.nextInt(1, leftlen)
            val scheduleActions = (0 until slen).map {
                sampleRandomScheduleTaskAction()
            }
            actions.addAll(0, scheduleActions)
        }

        val ind = createRPCIndividual(sampleType = SampleType.RANDOM, actions)
        ind.doGlobalInitialize(searchGlobalState)
        return ind
    }

    /*
        TODO Man: smart sampling for RPC
     */

    //  init a sequence of individual
    private fun initAdHocInitialIndividuals(infoDto: SutInfoDto){
        // create one action per individual with/without auth
        adHocInitialIndividuals.clear()
        createSingleCallIndividualOnEachAction()

        adHocInitialIndividuals.forEach {
            it.doGlobalInitialize(searchGlobalState)
        }
        adHocInitialIndividuals
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {
        if (!config.seedTestCases) {
            throw IllegalStateException("'seedTestCases' should be true when initializing seeded tests")
        }


        if (infoDto?.rpcProblem?.seededTestDtos?.isNotEmpty() == true){
            seededIndividuals.addAll(
                rpcHandler.handledSeededTests(infoDto.rpcProblem.seededTestDtos)
                    .map{
                        it.seeAllActions().forEach { a -> a.doInitialize() }
                        it
                    }
            )
        }

        seededIndividuals.forEach{
            it.doGlobalInitialize(searchGlobalState)
        }
    }

    private fun createSingleCallIndividualOnEachAction() {
        // create a test with one RPC call
        actionCluster.asSequence()
                .filter { a -> a.value is RPCCallAction }
                .forEach { a ->
                    val copy = a.value.copy() as RPCCallAction
                    copy.doInitialize(randomness)
                    rpcHandler.actionWithAllAuth(copy).forEach { actionWithAuth->
                        rpcHandler.actionWithAllCandidates(actionWithAuth)
                            .forEach { actionWithSeeded->
                                val a = EnterpriseActionGroup(mutableListOf(actionWithSeeded), RPCCallAction::class.java)
                                val ind = createRPCIndividual(SampleType.RANDOM, mutableListOf(a))
                                adHocInitialIndividuals.add(ind)
                        }
                    }
                }
        // create a test with one schedule task
        scheduleActionCluster.asSequence()
            .filter { it.value is ScheduleTaskAction }
            .forEach { a->
                val copy = a.value.copy() as ScheduleTaskAction
                copy.doInitialize(randomness)
                val ind = createSingleScheduleTaskRPCIndividual(SampleType.RANDOM, mutableListOf(copy))
                adHocInitialIndividuals.add(ind)
            }
    }

    private fun createSingleScheduleTaskRPCIndividual(sampleType: SampleType, scheduleTaskActions: MutableList<ScheduleTaskAction>) : RPCIndividual{
        return RPCIndividual(
            sampleType =  sampleType,
            trackOperator = if(config.trackingEnabled()) this else null,
            index = if (config.trackingEnabled()) time.evaluatedIndividuals else -1,
            actions = mutableListOf(),
            scheduleTaskActions = scheduleTaskActions.toMutableList()
        )
    }

    private fun createRPCIndividual(sampleType: SampleType, actions : MutableList<ActionComponent>) : RPCIndividual{
        // enable tracking in rpc
        return RPCIndividual(
            sampleType = sampleType,
            trackOperator = if(config.trackingEnabled()) this else null,
            index = if (config.trackingEnabled()) time.evaluatedIndividuals else -1,
            allActions=actions
        )
    }
}
