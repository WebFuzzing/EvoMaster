package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.api.service.ApiWsSampler
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
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

    @Inject(optional = true)
    protected lateinit var rc: RemoteController

    @Inject
    protected lateinit var rpcHandler: RPCEndpointsHandler


    protected val adHocInitialIndividuals: MutableList<RPCIndividual> = mutableListOf()

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
                ?: throw java.lang.IllegalStateException("Missing problem definition object")

        rpcHandler.initActionCluster(problem, actionCluster, infoDto)

        initSqlInfo(infoDto)

        initAdHocInitialIndividuals()

        updateConfigForTestOutput(infoDto)
        log.debug("Done initializing {}", RPCSampler::class.simpleName)
    }

    override fun initSqlInfo(infoDto: SutInfoDto) {
        if (infoDto.sqlSchemaDto != null && configuration.shouldGenerateSqlData()) {
            sqlInsertBuilder = SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
            existingSqlData = sqlInsertBuilder!!.extractExistingPKs()
        }
    }

    /**
     * sample an action from [actionCluster] at random
     * @param noAuthProbability specifies a probability which does not apply any auth
     */
    fun sampleRandomAction(noAuthProbability: Double = 0.05): RPCCallAction {
        val action = randomness.choose(actionCluster).copy() as RPCCallAction
        randomizeActionGenes(action)
        if (randomness.nextBoolean(noAuthProbability)){
            action.setNoAuth()
        }else
            rpcHandler.actionWithRandomAuth(action, randomness)
        return action
    }

    override fun sampleAtRandom(): RPCIndividual {
        val len = randomness.nextInt(1, config.maxTestSize)
        val actions = (0 until len).map { sampleRandomAction(0.05)}
        return createRPCIndividual(actions.toMutableList())
    }

    /*
        TODO Man: smart sampling for RPC
     */

    //  init a sequence of individual
    private fun initAdHocInitialIndividuals(){
        // create one action per individual with/without auth
        adHocInitialIndividuals.clear()
        createSingleCallIndividualOnEachAction()

        if (config.seedTestCases){
            // TODO handle seeded individual
            throw IllegalStateException("Seeding test cases is not support for RPC yet.")
        }
    }

    private fun createSingleCallIndividualOnEachAction() {
        actionCluster.asSequence()
                .filter { a -> a.value is RPCCallAction }
                .forEach { a ->
                    val copy = a.value.copy() as RPCCallAction
                    randomizeActionGenes(copy)
                    rpcHandler.actionWithAllAuth(copy).forEach { actionWithAuth->
                        val ind = createRPCIndividual(mutableListOf(actionWithAuth))
                        adHocInitialIndividuals.add(ind)
                    }
                }
    }

    private fun createRPCIndividual(actions : MutableList<RPCCallAction>) : RPCIndividual{
        // enable tracking in rpc
        return RPCIndividual(actions, trackOperator = if(config.trackingEnabled()) this else null, index = if (config.trackingEnabled()) time.evaluatedIndividuals else -1)
    }
}