package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.httpws.service.HttpWsSampler
import org.evomaster.core.problem.httpws.service.auth.AuthenticationInfo
import org.evomaster.core.problem.httpws.service.auth.NoAuth
import org.evomaster.core.problem.rpc.RPCAction
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * created by manzhang on 2021/11/25
 */
class RPCSampler: HttpWsSampler<RPCIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RPCSampler::class.java)
    }

    @Inject
    protected lateinit var configuration: EMConfig

    @Inject(optional = true)
    protected lateinit var rc: RemoteController

    @Inject
    protected lateinit var convertor: RPCDtoConvertor


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

        convertor.initActionCluster(problem, actionCluster)

        setupAuthentication(infoDto)
        initSqlInfo(infoDto)

        initAdHocInitialIndividuals()

        log.debug("Done initializing {}", RPCSampler::class.simpleName)
    }

    override fun initSqlInfo(infoDto: SutInfoDto) {
        if (infoDto.sqlSchemaDto != null && configuration.shouldGenerateSqlData()) {
            sqlInsertBuilder = SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
            existingSqlData = sqlInsertBuilder!!.extractExistingPKs()
        }
    }

    override fun sampleAtRandom(): RPCIndividual {
        val len = randomness.nextInt(1, config.maxTestSize)
        val actions = (0 until len).map { sampleRandomAction(0.05) as RPCAction }
        return createRPCIndividual(actions.toMutableList())
    }

    /*
        TODO Man: smart sampling for RPC
     */

    //  init a sequence of individual
    private fun initAdHocInitialIndividuals(){
        // create one action per individual with/without auth
        adHocInitialIndividuals.clear()
        createSingleCallIndividualOnEachAction(NoAuth())

        authentications.forEach { a->
            createSingleCallIndividualOnEachAction(a)
        }

        if (config.seedTestCases){
            // TODO handle seeded individual
            throw IllegalStateException("Seeding test cases is not support for RPC yet.")
        }
    }

    private fun createSingleCallIndividualOnEachAction(auth: AuthenticationInfo) {
        actionCluster.asSequence()
                .filter { a -> a.value is RPCAction }
                .forEach { a ->
                    val copy = a.value.copy() as RPCAction
                    copy.auth = auth
                    randomizeActionGenes(copy)
                    val ind = createRPCIndividual(mutableListOf(copy))
                    adHocInitialIndividuals.add(ind)
                }
    }

    private fun createRPCIndividual(actions : MutableList<RPCAction>) : RPCIndividual{
        // enable tracking in rpc
        return RPCIndividual(actions, trackOperator = if(config.trackingEnabled()) this else null, index = if (config.trackingEnabled()) time.evaluatedIndividuals else -1)
    }
}